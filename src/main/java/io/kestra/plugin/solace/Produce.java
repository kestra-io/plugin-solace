package io.kestra.plugin.solace;

import com.solace.messaging.MessagingService;
import com.solace.messaging.resources.Topic;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Metric;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Data;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.solace.client.MessagingServiceFactory;
import io.kestra.plugin.solace.data.InputStreamProvider;
import io.kestra.plugin.solace.serde.Serde;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.publisher.AbstractSolaceDirectMessagePublisher;
import io.kestra.plugin.solace.service.publisher.DeliveryModes;
import io.kestra.plugin.solace.service.publisher.SolaceDirectMessagePublisher;
import io.kestra.plugin.solace.service.publisher.SolacePersistentMessagePublisher;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static io.kestra.core.utils.Rethrow.throwFunction;

/**
 * The {@link RunnableTask} can be used for producing messages to a Solace Broker.
 */
@Plugin(
    examples = {
        @Example(
            title = "Publish a file as messages into a Solace Broker.",
            full = true,
            code = {
                """
                    id: send_messages_to_solace_queue
                    namespace: company.team

                    inputs:
                      - id: file
                        type: FILE
                        description: a CSV file with columns id, username, tweet, and timestamp

                    tasks:
                      - id: read_csv_file
                        type: io.kestra.plugin.serdes.csv.CsvToIon
                        from: "{{ inputs.file }}"

                      - id: transform_row_to_json
                        type: io.kestra.plugin.graalvm.js.FileTransform
                        from: "{{ outputs.read_csv_file.uri }}"
                        script: |
                          var result = {
                            "payload": {
                              "username": row.username,
                              "tweet": row.tweet
                            },
                            "properties": {
                                "correlationId": "42"
                            }
                          };
                          row = result

                      - id: send_message_to_solace
                        type: io.kestra.plugin.solace.Produce
                        from: "{{ outputs.transform_row_to_json.uri }}"
                        topicDestination: test/tweets
                        host: localhost:55555
                        username: admin
                        password: admin
                        vpn: default
                        messageSerializer: "JSON"
                    """
            }
        )
    },
    metrics = {
        @Metric(name = "messages", description = "Number of messages", type = Counter.TYPE),
    }
)
@Schema(
  title = "Publish messages to Solace topics",
  description = "Publishes one or more messages to a Solace Broker topic using the chosen serializer. Defaults to persistent delivery with a 1 minute acknowledgement wait; DIRECT skips acknowledgements." 
)
@SuperBuilder
@NoArgsConstructor
@Getter
public class Produce extends AbstractSolaceTask implements RunnableTask<Produce.Output>, Data.From {
    @Schema(
        title = "Message content",
        description = "Internal storage URI (`kestra://`), a map, or a list of maps to publish."
    )
    @NotNull
    private Object from;

    @Schema(title = "Topic destination", description = "Rendered topic string for all outgoing messages.")
    @NotNull
    private Property<String> topicDestination;

    @Schema(title = "Message serializer", description = "Serde used to encode payloads. Defaults to STRING.")
    @Builder.Default
    private Property<Serdes> messageSerializer = Property.ofValue(Serdes.STRING);

    @Schema(title = "Serializer properties", description = "Key/value configs passed to the serializer.")
    @Builder.Default
    protected Property<Map<String, Object>> messageSerializerProperties = Property.ofValue(new HashMap<>());

    @Schema(title = "Delivery mode", description = "DIRECT sends immediately; PERSISTENT waits for broker acknowledgement.")
    @Builder.Default
    private Property<DeliveryModes> deliveryMode = Property.ofValue(DeliveryModes.PERSISTENT);

    @Schema(title = "Acknowledgement timeout", description = "Max wait when deliveryMode is PERSISTENT. Defaults to 1 minute.")
    @NotNull
    @Builder.Default
    private Property<Duration> awaitAcknowledgementTimeout = Property.ofValue(Duration.ofMinutes(1));

    @Schema(title = "Message properties", description = """
        Optional properties applied to every message. Keys must be String and values String; supports Solace message properties.
        """
    )
    @Builder.Default
    protected Property<Map<String, String>> messageProperties = Property.ofValue(new HashMap<>());

    @Override
    public Output run(RunContext runContext) throws Exception {
        InputStreamProvider provider = new InputStreamProvider(runContext);

        int totalSentMessages = Data.from(from)
            .read(runContext)
            .map(throwFunction(row -> {
                try (InputStream is = provider.get(row)) {
                    return send(runContext, is).getMessagesCount();
                }
            }))
            .reduce(Integer::sum)
            .blockOptional()
            .orElse(0);

        return new Output(totalSentMessages);
    }


    private Output send(final RunContext runContext, final InputStream stream) throws Exception {
        final Serde serde = runContext.render(getMessageSerializer())
            .as(Serdes.class)
            .orElseThrow()
            .create(runContext.render(getMessageSerializerProperties()).asMap(String.class, Object.class));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            final Topic topic = Topic.of(runContext.render(topicDestination).as(String.class).orElseThrow());

            AbstractSolaceDirectMessagePublisher sender = switch (runContext.render(deliveryMode).as(DeliveryModes.class).orElseThrow()) {
                case DIRECT -> new SolaceDirectMessagePublisher(topic, serde, runContext.logger());
                case PERSISTENT -> new SolacePersistentMessagePublisher(
                    topic,
                    serde,
                    runContext.logger(),
                    runContext.render(awaitAcknowledgementTimeout).as(Duration.class).orElseThrow()
                );
            };

            MessagingService service = MessagingServiceFactory.create(this, runContext);
            AbstractSolaceDirectMessagePublisher.SendResult result = sender.send(
                reader,
                service,
                runContext.render(messageProperties).asMap(String.class, Object.class)
            );

            runContext.metric(Counter.of("messages", result.totalSentMessages()));

            return new Output(result.totalSentMessages());
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(title = "Total number of messages published by the task.")
        private final Integer messagesCount;
    }
}

package io.kestra.plugin.solace;

import com.solace.messaging.MessagingService;
import com.solace.messaging.resources.Topic;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.executions.metrics.Counter;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Output;
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
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@link RunnableTask} can be used for producing messages to a Solace Broker.
 */
@Plugin(examples = {
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
                    type: io.kestra.plugin.scripts.nashorn.FileTransform
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
})
@Schema(
    title = "Publish messages to a Solace Broker."
)
@Slf4j
@SuperBuilder
@NoArgsConstructor
@Getter
public class Produce extends AbstractSolaceTask implements RunnableTask<Output> {

    // TASK'S METRICS
    private static final String METRIC_PUBLISHED_MESSAGES_NAME = "total-published-messages";

    // TASK'S PROPERTIES
    @Schema(
        title = "The content of the message to be published to Solace",
        description = "Can be an internal storage URI, a map (i.e. a list of key-value pairs) or a list of maps. " +
            "The following keys are supported: `payload`, `properties`.",
        anyOf = {String.class, List.class, Map.class}
    )
    @NotNull
    @PluginProperty(dynamic = true)
    private Object from;

    @Schema(
        title = "The topic destination to publish messages."
    )
    @NotNull
    private Property<String> topicDestination;

    @Schema(
        title = "The Serializer to be used for serializing messages."
    )
    @Builder.Default
    private Property<Serdes> messageSerializer = Property.of(Serdes.STRING);

    @Schema(
        title = "The config properties to be passed to the Serializer.",
        description = "Configs in key/value pairs."
    )
    @Builder.Default
    protected Property<Map<String, Object>> messageSerializerProperties = Property.of(new HashMap<>());

    @Schema(
        title = "The delivery mode to be used for publishing messages messages."
    )
    @Builder.Default
    private Property<DeliveryModes> deliveryMode = Property.of(DeliveryModes.PERSISTENT);

    @Schema(
        title = "The maximum time to wait for the message acknowledgement (in milliseconds) when configuring `deliveryMode` to `PERSISTENT`."
    )
    @NotNull
    @Builder.Default
    private Property<Duration> awaitAcknowledgementTimeout = Property.of(Duration.ofMinutes(1));

    @Schema(
        title = "Additional properties to customize all messages to be published.",
        description = """
            Additional properties must be provided with Key of type String and Value of type String.
            Each key can be customer provided, or it can be a Solace message properties.
            """
    )
    @Builder.Default
    protected Property<Map<String, String>> messageProperties = Property.of(new HashMap<>());

    /**
     * {@inheritDoc}
     **/
    @Override
    public Output run(RunContext runContext) throws Exception {
        final InputStreamProvider provider = new InputStreamProvider(runContext);

        InputStream is = null;
        if (this.getFrom() instanceof String uri) {
            is = provider.get(runContext.render(uri));
        }

        if (this.getFrom() instanceof Map data) {
            is = provider.get(data);
        }

        if (this.getFrom() instanceof List data) {
            is = provider.get(data);
        }

        if (is == null) {
            throw new IllegalArgumentException(
                "Unsupported type for task-property `from`: " + this.getFrom().getClass().getSimpleName()
            );
        }

        return send(runContext, is);
    }

    private Output send(final RunContext runContext, final InputStream stream) throws Exception {
        final Serde serde = runContext.render(getMessageSerializer()).as(Serdes.class).orElseThrow()
            .create(runContext.render(getMessageSerializerProperties()).asMap(String.class, Object.class));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            final Topic topic = Topic.of(runContext.render(topicDestination).as(String.class).orElseThrow());
            AbstractSolaceDirectMessagePublisher sender = switch (runContext.render(deliveryMode).as(DeliveryModes.class).orElseThrow()) {
                case DIRECT -> new SolaceDirectMessagePublisher(
                    topic,
                    serde,
                    runContext.logger()
                );
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

            // metrics
            runContext.metric(Counter.of(METRIC_PUBLISHED_MESSAGES_NAME, result.totalSentMessages()));

            return new Output(result.totalSentMessages());
        }
    }

    @AllArgsConstructor
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Total number of messages published by the task."
        )
        private final Integer messagesCount;
    }
}

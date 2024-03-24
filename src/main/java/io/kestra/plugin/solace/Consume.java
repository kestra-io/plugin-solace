package io.kestra.plugin.solace;

import com.solace.messaging.MessagingService;
import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.runners.RunContext;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.solace.client.MessagingServiceFactory;
import io.kestra.plugin.solace.serde.Serde;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.receiver.QueueTypes;
import io.kestra.plugin.solace.service.receiver.ReceiverContext;
import io.kestra.plugin.solace.service.receiver.SolacePersistentMessageReceiver;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import jakarta.validation.constraints.NotNull;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;

/**
 * The {@link RunnableTask} can be used for consuming messages from Solace.
 */
@Plugin(examples = {
    @Example(
        title = "Consume messages from a Solace queue.",
        full = true,
        code = {
            """
                id: ConsumeMessageFromSolaceQueue
                namespace: company.team
                tasks:
                - id: consumeFomSolace
                  type: io.kestra.plugin.solace.Consume
                  host: localhost:55555
                  username: admin
                  password: admin
                  vpn: default
                  messageDeserializer: JSON
                  queueName: test_queue
                  queueType: DURABLE_EXCLUSIVE
                """
        }
    )
})
@Schema(
    title = "Consume messages from a Solace broker."
)
@NoArgsConstructor
@SuperBuilder
@Getter
public class Consume extends AbstractSolaceTask implements SolaceConsumeInterface, RunnableTask<Consume.Output> {

    // TASK'S METRICS
    private static final String METRIC_SENT_MESSAGES_NAME = "total-received-messages";

    // TASK'S PROPERTIES
    @NotNull
    private String queueName;

    @NotNull
    private QueueTypes queueType;

    @Builder.Default
    private Serdes messageDeserializer = Serdes.STRING;

    @Builder.Default
    private Map<String, Object> messageDeserializerProperties = Collections.emptyMap();

    @Builder.Default
    private Integer maxMessages = 100;

    @Builder.Default
    private Duration maxDuration = Duration.ofSeconds(10);

    private String messageSelector;

    /**
     * {@inheritDoc}
     **/
    @Override
    public Output run(RunContext runContext) throws Exception {
        return run(runContext, this);
    }

    Output run(RunContext runContext, SolaceConsumeInterface task) throws Exception {

        File tempFile = runContext.tempFile(".ion").toFile();
        try (
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(tempFile))
        ) {
            final Serde serde = task
                .getMessageDeserializer()
                .create(task.getMessageDeserializerProperties());
            final MessagingService service = MessagingServiceFactory.create(task.render(runContext));
            final Logger logger = runContext.logger();
            SolacePersistentMessageReceiver receiver = new SolacePersistentMessageReceiver(serde, logger);

            final String queueName = runContext.render(task.getQueueName());

            int totalReceivedMessages = receiver.poll(
                service,
                new ReceiverContext(
                    task.getMaxDuration(),
                    task.getMaxMessages(),
                    task.getMessageSelector()
                ),
                task.getQueueType().get(queueName),
                message -> {
                    try {
                        FileSerde.write(output, message);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

            output.flush();
            return new Output(totalReceivedMessages, runContext.putTempFile(tempFile));
        }
    }

    @Builder
    @Getter
    public static class Output implements io.kestra.core.models.tasks.Output {
        @Schema(
            title = "Number of messages consumed from the Solace broker."
        )
        private final Integer messagesCount;

        @Schema(
            title = "URI of a Kestra's internal storage file containing the messages."
        )
        private URI uri;
    }
}

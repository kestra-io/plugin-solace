package io.kestra.plugin.solace;

import com.solace.messaging.MessagingService;
import com.solace.messaging.resources.Topic;
import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.solace.client.MessagingServiceFactory;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.publisher.SolacePersistentMessagePublisher;
import io.kestra.plugin.solace.service.receiver.QueueTypes;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.solace.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.Duration;
import java.util.Collections;

@KestraTest
class ConsumeTest extends BaseSolaceIT {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumeTest.class);

    public static final String TEST_QUEUE = "test";
    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testConsumerTask() throws Exception {
        // Given
        RunContext runContext = runContextFactory.of();
        createQueueWithSubscriptionTopic(TEST_QUEUE, "topic");

        Consume task = Consume.builder()
            .messageDeserializer(Property.of(Serdes.STRING))
            .username(Property.of(SOLACE_USER))
            .password(Property.of(SOLACE_PASSWORD))
            .vpn(Property.of(SOLACE_VPN))
            .host(Property.of(solaceContainer.getOrigin(Service.SMF)))
            .maxDuration(Property.of(Duration.ofSeconds(5)))
            .maxMessages(Property.of(1))
            .queueName(Property.of(TEST_QUEUE))
            .queueType(Property.of(QueueTypes.DURABLE_EXCLUSIVE))
            .build();

        try (BufferedReader message = new BufferedReader(new StringReader("""
            {"payload": "test-message"}
            """))) {
            MessagingService service = MessagingServiceFactory.create(task, runContext);
            SolacePersistentMessagePublisher publisher = new SolacePersistentMessagePublisher(
                Topic.of("topic"),
                Serdes.STRING.create(Collections.emptyMap()),
                LOG,
                Duration.ofSeconds(5)
            );
            var result = publisher.send(message, service, Collections.emptyMap());
            Assertions.assertEquals(1, result.totalSentMessages()); // assert message was published.
        }

        // When
        Consume.Output runOutput = task.run(runContext);

        // Then
        Assertions.assertEquals(1, runOutput.getMessagesCount());
    }
}

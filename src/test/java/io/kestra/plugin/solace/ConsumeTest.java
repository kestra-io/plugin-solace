package io.kestra.plugin.solace;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.solace.Service;

import com.solace.messaging.MessagingService;
import com.solace.messaging.resources.Topic;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.solace.client.MessagingServiceFactory;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.publisher.SolacePersistentMessagePublisher;
import io.kestra.plugin.solace.service.receiver.QueueTypes;

import jakarta.inject.Inject;

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
            .messageDeserializer(Property.ofValue(Serdes.STRING))
            .username(Property.ofValue(SOLACE_USER))
            .password(Property.ofValue(SOLACE_PASSWORD))
            .vpn(Property.ofValue(SOLACE_VPN))
            .host(Property.ofValue(solaceContainer.getOrigin(Service.SMF)))
            .maxDuration(Property.ofValue(Duration.ofSeconds(5)))
            .maxMessages(Property.ofValue(1))
            .queueName(Property.ofValue(TEST_QUEUE))
            .queueType(Property.ofValue(QueueTypes.DURABLE_EXCLUSIVE))
            .build();

        try (InputStream message = new ByteArrayInputStream("""
            {"payload": "test-message"}
            """.getBytes(StandardCharsets.UTF_8))) {
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

        // Verify ION read-back from storage
        try (InputStream is = new BufferedInputStream(runContext.storage().getFile(runOutput.getUri()), FileSerde.BUFFER_SIZE)) {
            List<Object> result = new ArrayList<>();
            FileSerde.read(is, result::add);
            Assertions.assertEquals(1, result.size());
        }
    }
}

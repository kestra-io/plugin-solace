package io.kestra.plugin.solace.service.publisher;

import com.solace.messaging.MessagingService;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.MessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.resources.Topic;
import io.kestra.plugin.solace.serde.Serde;
import org.slf4j.Logger;

import java.util.Objects;

public final class SolaceDirectMessagePublisher extends AbstractSolaceDirectMessagePublisher {

    private static final int DEFAULT_BACKPRESSURE_BUFFER_SIZE = 1;
    private DirectMessagePublisher publisher;

    private final Topic topic;

    public SolaceDirectMessagePublisher(Topic topic, Serde serde, Logger logger) {
        super(serde, logger);
        this.topic = Objects.requireNonNull(topic, "topic cannot be null");
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    protected MessagePublisher open(final MessagingService messagingService) {
        publisher = messagingService
            .createDirectMessagePublisherBuilder()
            .onBackPressureWait(DEFAULT_BACKPRESSURE_BUFFER_SIZE)
            .build()
            .start();
        publisher.start();
        return publisher;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    protected void publish(final OutboundMessage message) {
        publisher.publish(message, topic);
    }
}

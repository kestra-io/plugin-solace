package io.kestra.plugin.solace.service.publisher;

import com.solace.messaging.MessagingService;
import com.solace.messaging.publisher.MessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import com.solace.messaging.publisher.PersistentMessagePublisher;
import com.solace.messaging.resources.Topic;
import io.kestra.plugin.solace.serde.Serde;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.Objects;

public final class SolacePersistentMessagePublisher extends AbstractSolaceDirectMessagePublisher {

    private static final int DEFAULT_BACKPRESSURE_BUFFER_SIZE = 1;

    private PersistentMessagePublisher publisher;

    private final Topic topic;

    private final Duration awaitAcknowledgementTimeout;

    public SolacePersistentMessagePublisher(final Topic topic,
                                            final Serde serde,
                                            final Logger logger,
                                            final Duration awaitAcknowledgementTimeout) {
        super(serde, logger);
        this.topic = Objects.requireNonNull(topic, "topic cannot be null");
        this.awaitAcknowledgementTimeout = awaitAcknowledgementTimeout;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    protected MessagePublisher open(final MessagingService messagingService) {
        publisher = messagingService
            .createPersistentMessagePublisherBuilder()
            .withDeliveryAckWindowSize(1)
            .onBackPressureWait(DEFAULT_BACKPRESSURE_BUFFER_SIZE)
            .build()
            .start();

        publisher.setMessagePublishReceiptListener(publishReceipt -> {
            if (logger().isTraceEnabled()) {
                if (publishReceipt.isPersisted()) {
                    logger().trace("Message reached a broker and persistence confirmation was received back.");
                }
            }
        });
        return publisher;
    }

    /**
     * {@inheritDoc}
     **/
    @Override
    protected void publish(final OutboundMessage message) throws InterruptedException {
        publisher.publishAwaitAcknowledgement(message, topic, awaitAcknowledgementTimeout.toMillis());
    }
}

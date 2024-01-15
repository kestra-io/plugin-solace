package io.kestra.plugin.solace.service.receiver;

import com.solace.messaging.MessagingService;
import com.solace.messaging.PersistentMessageReceiverBuilder;
import com.solace.messaging.receiver.InboundMessage;
import com.solace.messaging.receiver.PersistentMessageReceiver;
import com.solace.messaging.resources.Queue;
import io.kestra.plugin.solace.serde.Serde;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for consuming messages.
 */
public class SolacePersistentMessageReceiver {

    private final Serde serde;
    private final Logger logger;

    /**
     * Creates a new {@link SolacePersistentMessageReceiver} instance.
     *
     * @param serde  The serde for message payload - must not be {@code null}.
     * @param logger The logger - must not be {@code null}.
     */
    public SolacePersistentMessageReceiver(final Serde serde, final Logger logger) {
        this.serde = Objects.requireNonNull(serde, "serde cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    /**
     * Polls messages from the given Solace queue.
     *
     * @param messagingService The {@link MessagingService}.
     * @param context          The receiver context.
     * @param listener         The message listener.
     */
    public int poll(final MessagingService messagingService,
                    final ReceiverContext context,
                    final Queue queue,
                    final MessageListener listener
    ) {

        try {
            final long maxDurationInMillis = context.maxDuration().toMillis();
            final long start = System.currentTimeMillis();

            PersistentMessageReceiverBuilder builder = messagingService.createPersistentMessageReceiverBuilder();
            Optional.ofNullable(context.messageSelector())
                .ifPresent(builder::withMessageSelector);

            final PersistentMessageReceiver receiver = builder
                .build(queue)
                .start();

            long timeElapsedInMillis;
            int totalReceivedMessages = 0;
            do {
                timeElapsedInMillis = System.currentTimeMillis() - start;
                long maxTimeout = Math.max(0, maxDurationInMillis - timeElapsedInMillis);

                InboundMessage inboundMessage = receiver.receiveMessage(maxTimeout);
                if (inboundMessage != null) {
                    Object payload = serde.deserialize(inboundMessage.getPayloadAsBytes());
                    InboundMessageObject message = new InboundMessageObject(
                        inboundMessage.getSenderId(),
                        inboundMessage.getSenderTimestamp(),
                        inboundMessage.getDestinationName(),
                        inboundMessage.getApplicationMessageId(),
                        inboundMessage.getApplicationMessageType(),
                        inboundMessage.getCorrelationId(),
                        inboundMessage.isRedelivered(),
                        payload,
                        inboundMessage.getProperties()

                    );
                    listener.onMessage(message);
                    totalReceivedMessages++;
                    receiver.ack(inboundMessage);
                }
            } while (!isCompleted(context, totalReceivedMessages, timeElapsedInMillis));

            logger.debug("Received {} messages in {} milliseconds.", totalReceivedMessages, timeElapsedInMillis);
            return totalReceivedMessages;
        } finally {
            messagingService.disconnect();
        }
    }

    private static boolean isCompleted(ReceiverContext context,
                                       int totalReceivedMessages,
                                       long timeElapsedInMillis) {
        return totalReceivedMessages >= context.maxMessages() || timeElapsedInMillis >= context.maxDuration().toMillis();
    }

    public interface MessageListener {

        void onMessage(final InboundMessageObject message);
    }

    /**
     * Represents a serializable {@link com.solace.messaging.receiver.InboundMessage}.
     */
    public record InboundMessageObject(
        String senderId,
        Long senderTimestamp,
        String destinationName,
        String applicationMessageId,
        String applicationMessageType,
        String correlationId,
        Boolean isRedelivered,
        Object payload,
        Map<String, String> properties
    ) {
    }

}

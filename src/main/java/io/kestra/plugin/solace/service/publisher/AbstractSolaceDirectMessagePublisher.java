package io.kestra.plugin.solace.service.publisher;

import com.solace.messaging.MessagingService;
import com.solace.messaging.publisher.DirectMessagePublisher;
import com.solace.messaging.publisher.MessagePublisher;
import com.solace.messaging.publisher.OutboundMessage;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.solace.serde.Serde;
import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public abstract class AbstractSolaceDirectMessagePublisher {

    public static final long DEFAULT_TERMINATE_TIMEOUT = Duration.ofMinutes(1).toMillis();
    private final Serde serde;
    private final Logger logger;

    /**
     * Creates a new {@link AbstractSolaceDirectMessagePublisher} instance.
     *
     * @param serde The serde to be used for converting message payload to bytes.
     */
    public AbstractSolaceDirectMessagePublisher(Serde serde, Logger logger) {
        this.serde = Objects.requireNonNull(serde, "serde cannot be null");
        this.logger = Objects.requireNonNull(logger, "logger cannot be null");
    }

    protected Logger logger() {
        return logger;
    }

    /**
     * Publishes all messages from the given reader.
     *
     * @param reader                      The reader used to retrieve messages to be sent.
     * @param messagingService            The {@link MessagingService} used to build a new {@link DirectMessagePublisher}.
     * @param additionalMessageProperties The additional message properties to customize all messages to b published.
     * @return a new {@link SendResult}.
     */
    public SendResult send(BufferedReader reader,
                           MessagingService messagingService,
                           Map<String, String> additionalMessageProperties) {

        MessagePublisher publisher = open(messagingService);
        logger.debug("Connected to Solace instance name {}", publisher.publisherInfo().getInstanceName());
        try (reader) {
            Flowable<OutboundMessageObject> flowable = Flowable.create(
                FileSerde.reader(reader, OutboundMessageObject.class),
                BackpressureStrategy.BUFFER
            );
            final Integer numSentMessages = flowable
                .map(outboundMessageObject -> {
                    final OutboundMessage message = buildOutboundMessage(
                        messagingService,
                        outboundMessageObject,
                        additionalMessageProperties
                    );
                    publish(message);
                    return 1;
                })
                .reduce(Integer::sum)
                .blockingGet();
            terminate(publisher);
            return new SendResult(numSentMessages);
        } catch (Exception e) {
            terminate(publisher);
            throw new RuntimeException(e);
        } finally {
            messagingService.disconnect();
        }
    }

    private void terminate(MessagePublisher publisher) {
        publisher.terminate(DEFAULT_TERMINATE_TIMEOUT);
    }

    /**
     * Opens the publisher.
     */
    protected abstract MessagePublisher open(final MessagingService messagingService);

    /**
     * Publishes the given message - this method should block until message is published.
     *
     * @param message The message to be published.
     */
    protected abstract void publish(final OutboundMessage message) throws Exception;

    private OutboundMessage buildOutboundMessage(MessagingService messagingService,
                                                 OutboundMessageObject object,
                                                 Map<String, String> additionalMessageProperties) {
        Properties properties = new Properties();
        Optional.ofNullable(additionalMessageProperties).ifPresent(properties::putAll);
        Optional.ofNullable(object.properties()).ifPresent(properties::putAll);

        byte[] payloadAsBytes = Optional.ofNullable(object.payload())
            .map(serde::serialize)
            .orElseGet(() -> new byte[0]);
        return messagingService
            .messageBuilder()
            .build(payloadAsBytes, properties);
    }

    /**
     * Result of a send operation.
     *
     * @param totalSentMessages The total number of messages sent to Solace.
     */
    public record SendResult(int totalSentMessages) {
    }

    /**
     * Represents a serializable {@link com.solace.messaging.receiver.InboundMessage}.
     */
    public record OutboundMessageObject(Object payload, Map<String, String> properties) {
    }
}

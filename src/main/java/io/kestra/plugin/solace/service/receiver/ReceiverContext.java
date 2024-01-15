package io.kestra.plugin.solace.service.receiver;

import java.time.Duration;

/**
 * Wraps all options for receiving messages.
 *
 * @param maxDuration     The maximum number of messages to be received per poll.
 * @param maxMessages     The maximum time to wait for receiving messages.
 * @param messageSelector The message selector to be used for receiving messages.
 */
public record ReceiverContext(Duration maxDuration,
                              Integer maxMessages,
                              String messageSelector) {
}

package io.kestra.plugin.solace;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.receiver.QueueTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.Duration;
import java.util.Map;

public interface SolaceConsumeInterface extends SolaceConnectionInterface {

    @Schema(
        title = "The name of the solace queue to consume from."
    )
    @NotNull
    Property<String> getQueueName();

    @Schema(
        title = "The type of the queue to be consumed."
    )
    @NotNull
    Property<QueueTypes> getQueueType();

    @Schema(
        title = "The Deserializer to be used for deserializing messages."
    )
    @NotNull
    Property<Serdes> getMessageDeserializer();

    @Schema(
        title = "The config properties to be passed to the Deserializer.",
        description = "Configs in key/value pairs."
    )
    Property<Map<String, Object>> getMessageDeserializerProperties();

    @Schema(
        title = "The maximum number of messages to be received per poll."
    )
    Property<Integer> getMaxMessages();

    @Schema(
        title = "The maximum time to wait for receiving a number of messages up to `maxMessages`."
    )
    Property<Duration> getMaxDuration();

    @Schema(
        title = "The message selector to be used for receiving messages.",
        description = "Enables support for message selection based on message header parameter and message properties values."
    )
    Property<String> getMessageSelector();
}

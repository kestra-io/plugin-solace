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
        title = "Queue name",
        description = "Solace queue to consume from."
    )
    @NotNull
    Property<String> getQueueName();

    @Schema(
        title = "Queue type",
        description = "Durability and access mode for the queue."
    )
    @NotNull
    Property<QueueTypes> getQueueType();

    @Schema(
        title = "Message deserializer",
        description = "Serde used to decode messages. Defaults to STRING."
    )
    @NotNull
    Property<Serdes> getMessageDeserializer();

    @Schema(
        title = "Deserializer properties",
        description = "Key/value configs passed to the deserializer."
    )
    Property<Map<String, Object>> getMessageDeserializerProperties();

    @Schema(
        title = "Maximum messages",
        description = "Upper bound of messages per poll. Defaults to 100."
    )
    Property<Integer> getMaxMessages();

    @Schema(
        title = "Maximum duration",
        description = "Max poll duration. Defaults to 10 seconds."
    )
    Property<Duration> getMaxDuration();

    @Schema(
        title = "Message selector",
        description = "Solace selector expression to filter messages on headers/properties."
    )
    Property<String> getMessageSelector();
}

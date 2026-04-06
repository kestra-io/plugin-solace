package io.kestra.plugin.solace;

import java.time.Duration;
import java.util.Map;

import io.kestra.core.models.property.Property;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.receiver.QueueTypes;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import io.kestra.core.models.annotations.PluginProperty;

public interface SolaceConsumeInterface extends SolaceConnectionInterface {

    @Schema(
        title = "Queue name",
        description = "Solace queue to consume from."
    )
    @NotNull
    @PluginProperty(group = "main")
    Property<String> getQueueName();

    @Schema(
        title = "Queue type",
        description = "Durability and access mode for the queue."
    )
    @NotNull
    @PluginProperty(group = "main")
    Property<QueueTypes> getQueueType();

    @Schema(
        title = "Message deserializer",
        description = "Serde used to decode messages. Defaults to STRING."
    )
    @NotNull
    @PluginProperty(group = "main")
    Property<Serdes> getMessageDeserializer();

    @Schema(
        title = "Deserializer properties",
        description = "Key/value configs passed to the deserializer."
    )
    @PluginProperty(group = "advanced")
    Property<Map<String, Object>> getMessageDeserializerProperties();

    @Schema(
        title = "Maximum messages",
        description = "Upper bound of messages per poll. Defaults to 100."
    )
    @PluginProperty(group = "execution")
    Property<Integer> getMaxMessages();

    @Schema(
        title = "Maximum duration",
        description = "Max poll duration. Defaults to 10 seconds."
    )
    @PluginProperty(group = "execution")
    Property<Duration> getMaxDuration();

    @Schema(
        title = "Message selector",
        description = "Solace selector expression to filter messages on headers/properties."
    )
    @PluginProperty(group = "advanced")
    Property<String> getMessageSelector();
}

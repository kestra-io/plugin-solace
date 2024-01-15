package io.kestra.plugin.solace;

import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.receiver.QueueTypes;
import io.swagger.v3.oas.annotations.media.Schema;

import javax.validation.constraints.NotNull;
import java.time.Duration;
import java.util.Map;

public interface SolaceConsumeInterface extends SolaceConnectionInterface {

    @Schema(
        title = "The name of the solace queue to consume from."
    )
    @NotNull
    @PluginProperty(dynamic = true)
    String getQueueName();

    @Schema(
        title = "The type of the queue to be consumed."
    )
    @NotNull
    @PluginProperty
    QueueTypes getQueueType();

    @Schema(
        title = "The Deserializer to be used for deserializing messages."
    )
    @NotNull
    @PluginProperty
    Serdes getMessageDeserializer();

    @Schema(
        title = "The config properties to be passed to the Deserializer.",
        description = "Configs in key/value pairs."
    )
    @PluginProperty
    Map<String, Object> getMessageDeserializerProperties();

    @Schema(
        title = "The maximum number of messages to be received per poll."
    )
    @PluginProperty
    Integer getMaxMessages();

    @Schema(
        title = "The maximum time to wait for receiving a number of messages up to `maxMessages`."
    )
    @PluginProperty
    Duration getMaxDuration();

    @Schema(
        title = "The message selector to be used for receiving messages.",
        description = "Enables support for message selection based on message header parameter and message properties values."
    )
    @PluginProperty
    String getMessageSelector();
}

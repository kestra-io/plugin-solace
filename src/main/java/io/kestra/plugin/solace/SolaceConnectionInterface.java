package io.kestra.plugin.solace;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public interface SolaceConnectionInterface {

    @Schema(
        title = "Solace username"
    )
    Property<String> getUsername() throws IllegalVariableEvaluationException;

    @Schema(
        title = "Solace password"
    )
    Property<String> getPassword() throws IllegalVariableEvaluationException;

    @Schema(
        title = "Solace VPN",
        description = "VPN name to connect to. Defaults to `default` when not overridden."
    )
    @NotNull
    Property<String> getVpn() throws IllegalVariableEvaluationException;

    @Schema(
        title = "Solace host",
        description = "Broker hostname and port, for example `localhost:55555`."
    )
    @NotNull
    Property<String> getHost() throws IllegalVariableEvaluationException;

    @Schema(
        title = "Connection properties",
        description = "Additional broker connection properties in key/value pairs."
    )
    Property<Map<String, String>> getProperties() throws IllegalVariableEvaluationException;
}

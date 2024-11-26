package io.kestra.plugin.solace;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.property.Property;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public interface SolaceConnectionInterface {

    @Schema(
        title = "The Solace username."
    )
    Property<String> getUsername() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace password."
    )
    Property<String> getPassword() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace VPN to connect with."
    )
    @NotNull
    Property<String> getVpn() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace hostname to connect with."
    )
    @NotNull
    Property<String> getHost() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace properties to be used for connecting to the broker."
    )
    Property<Map<String, String>> getProperties() throws IllegalVariableEvaluationException;
}

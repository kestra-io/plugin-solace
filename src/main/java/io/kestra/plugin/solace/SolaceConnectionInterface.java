package io.kestra.plugin.solace;

import io.kestra.core.exceptions.IllegalVariableEvaluationException;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.runners.RunContext;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public interface SolaceConnectionInterface {

    @Schema(
        title = "The Solace username."
    )
    @PluginProperty(dynamic = true)
    String getUsername() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace password."
    )
    @PluginProperty(dynamic = true)
    String getPassword() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace VPN to connect with."
    )
    @NotNull
    @PluginProperty(dynamic = true)
    String getVpn() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace hostname to connect with."
    )
    @NotNull
    @PluginProperty(dynamic = true)
    String getHost() throws IllegalVariableEvaluationException;

    @Schema(
        title = "The Solace properties to be used for connecting to the broker."
    )
    @PluginProperty(dynamic = true)
    Map<String, String> getProperties() throws IllegalVariableEvaluationException;


    default SolaceConnectionInterface render(RunContext runContext) {

        final SolaceConnectionInterface delegate = this;
        return new SolaceConnectionInterface() {
            @Override
            public String getUsername() throws IllegalVariableEvaluationException {
                return render(delegate.getUsername());
            }

            @Override
            public String getPassword() throws IllegalVariableEvaluationException {
                return render(delegate.getPassword());
            }

            @Override
            public String getVpn() throws IllegalVariableEvaluationException {
                return render(delegate.getVpn());
            }

            @Override
            public String getHost() throws IllegalVariableEvaluationException {
                return render(delegate.getHost());
            }

            @Override
            public Map<String, String> getProperties() throws IllegalVariableEvaluationException {
                Map<String, String> properties = delegate.getProperties();
                if (properties == null || properties.isEmpty()) return properties;
                return runContext.renderMap(properties);
            }

            private String render(String input) throws IllegalVariableEvaluationException {
                return runContext.render(input);
            }
        };
    }
}

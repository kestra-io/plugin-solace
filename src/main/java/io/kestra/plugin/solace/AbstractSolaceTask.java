package io.kestra.plugin.solace;

import io.kestra.core.models.tasks.Task;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Collections;
import java.util.Map;

@NoArgsConstructor
@SuperBuilder
@Getter
public class AbstractSolaceTask extends Task implements SolaceConnectionInterface {

    private String username;

    private String password;

    @Builder.Default
    private String vpn = "default";

    private String host;

    @Builder.Default
    private Map<String, String> properties = Collections.emptyMap();
}

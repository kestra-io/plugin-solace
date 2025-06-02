package io.kestra.plugin.solace;

import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.Task;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@SuperBuilder
@Getter
abstract class AbstractSolaceTask extends Task implements SolaceConnectionInterface {

    private Property<String> username;

    private Property<String> password;

    @Builder.Default
    private Property<String> vpn = Property.ofValue("default");

    private Property<String> host;

    @Builder.Default
    private Property<Map<String, String>> properties = Property.ofValue(new HashMap<>());
}

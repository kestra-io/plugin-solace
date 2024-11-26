package io.kestra.plugin.solace;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.property.Property;
import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.publisher.DeliveryModes;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.solace.Service;

import java.util.List;
import java.util.Map;

@KestraTest
class ProduceTest extends BaseSolaceIT {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    void testGivenFromMap() throws Exception {
        RunContext runContext = runContextFactory.of();

        Produce task = Produce.builder()
            .from(Map.of("payload", "msg"))
            .messageSerializer(Property.of(Serdes.STRING))
            .username(Property.of(solaceContainer.getUsername()))
            .password(Property.of(solaceContainer.getPassword()))
            .vpn(Property.of(solaceContainer.getVpn()))
            .host(Property.of(solaceContainer.getOrigin(Service.SMF)))
            .deliveryMode(Property.of(DeliveryModes.DIRECT))
            .topicDestination(Property.of("topic"))
            .build();

        Produce.Output runOutput = task.run(runContext);

        Assertions.assertEquals(1, runOutput.getMessagesCount());
    }

    @Test
    void testGivenFromList() throws Exception {
        RunContext runContext = runContextFactory.of();

        Produce task = Produce.builder()
            .from(List.of(
                Map.of("payload", "msg1"),
                Map.of("payload", "msg2")
            ))
            .messageSerializer(Property.of(Serdes.STRING))
            .username(Property.of(solaceContainer.getUsername()))
            .password(Property.of(solaceContainer.getPassword()))
            .vpn(Property.of(solaceContainer.getVpn()))
            .host(Property.of(solaceContainer.getOrigin(Service.SMF)))
            .deliveryMode(Property.of(DeliveryModes.DIRECT))
            .topicDestination(Property.of("topic"))
            .build();

        Produce.Output runOutput = task.run(runContext);

        Assertions.assertEquals(2, runOutput.getMessagesCount());
    }
}

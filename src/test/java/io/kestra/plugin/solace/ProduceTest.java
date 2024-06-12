package io.kestra.plugin.solace;

import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.publisher.DeliveryModes;
import io.kestra.core.junit.annotations.KestraTest;
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
            .messageSerializer(Serdes.STRING)
            .username(solaceContainer.getUsername())
            .password(solaceContainer.getPassword())
            .vpn(solaceContainer.getVpn())
            .host(solaceContainer.getOrigin(Service.SMF))
            .deliveryMode(DeliveryModes.DIRECT)
            .topicDestination("topic")
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
            .messageSerializer(Serdes.STRING)
            .username(solaceContainer.getUsername())
            .password(solaceContainer.getPassword())
            .vpn(solaceContainer.getVpn())
            .host(solaceContainer.getOrigin(Service.SMF))
            .deliveryMode(DeliveryModes.DIRECT)
            .topicDestination("topic")
            .build();

        Produce.Output runOutput = task.run(runContext);

        Assertions.assertEquals(2, runOutput.getMessagesCount());
    }
}

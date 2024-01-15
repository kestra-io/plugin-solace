package io.kestra.plugin.solace.service.receiver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class QueueTypesTest {

    @Test
    void shouldCreateDurableExclusive() {
        Assertions.assertNotNull(QueueTypes.DURABLE_EXCLUSIVE.get("test"));
    }

    @Test
    void shouldCreateDurableNonExclusive() {
        Assertions.assertNotNull(QueueTypes.DURABLE_NON_EXCLUSIVE.get("test"));
    }

    @Test
    void shouldCreateNonDurableExclusive() {
        Assertions.assertNotNull(QueueTypes.NON_DURABLE_EXCLUSIVE.get("test"));
    }
}
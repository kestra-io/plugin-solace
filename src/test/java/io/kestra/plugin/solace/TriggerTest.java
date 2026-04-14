package io.kestra.plugin.solace;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.solace.Service;

import com.google.common.collect.ImmutableMap;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.property.Property;
import io.kestra.core.queues.QueueFactoryInterface;
import io.kestra.core.queues.QueueInterface;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import reactor.core.publisher.Flux;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

@KestraTest(startRunner = true, startScheduler = true)
class TriggerTest extends BaseSolaceIT {

    static final String TEST_QUEUE = "test";

    @Inject
    @Named(QueueFactoryInterface.EXECUTION_NAMED)
    private QueueInterface<Execution> executionQueue;

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    protected LocalFlowRepositoryLoader repositoryLoader;

    @Test
    void testTriggerTask() throws Exception {
        CountDownLatch queueCount = new CountDownLatch(1);
        Flux<Execution> receive = TestsUtils.receive(executionQueue, execution -> {
            queueCount.countDown();
            assertThat(execution.getLeft().getFlowId(), is("trigger"));
        });

        createQueueWithSubscriptionTopic(TEST_QUEUE, "topic");

        Produce task = Produce.builder()
            .id(TriggerTest.class.getSimpleName())
            .type(Produce.class.getName())
            .username(Property.ofValue(SOLACE_USER))
            .password(Property.ofValue(SOLACE_PASSWORD))
            .vpn(Property.ofValue(SOLACE_VPN))
            .host(Property.ofValue(solaceContainer.getOrigin(Service.SMF)))
            .topicDestination(Property.ofValue("topic"))
            .from(
                List.of(
                    ImmutableMap.builder()
                        .put("payload", "value1")
                        .build(),
                    ImmutableMap.builder()
                        .put("payload", "value2")
                        .build()
                )
            )
            .build();

        repositoryLoader.load(Objects.requireNonNull(TriggerTest.class.getClassLoader().getResource("flows")));

        task.run(TestsUtils.mockRunContext(runContextFactory, task, ImmutableMap.of()));

        boolean await = queueCount.await(1, TimeUnit.MINUTES);
        assertThat(await, is(true));

        Integer trigger = (Integer) receive.blockLast().getTrigger().getVariables().get("messagesCount");

        assertThat(trigger, greaterThanOrEqualTo(2));
    }
}

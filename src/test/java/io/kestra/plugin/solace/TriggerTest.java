package io.kestra.plugin.solace;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.testcontainers.solace.Service;

import com.google.common.collect.ImmutableMap;

import io.kestra.core.junit.annotations.KestraTest;
import java.time.Duration;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.runners.TestRunnerUtils;
import io.kestra.core.models.property.Property;
import io.kestra.core.queues.DispatchQueueInterface;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.TestsUtils;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static io.kestra.core.tenant.TenantService.MAIN_TENANT;

@KestraTest(startRunner = true, startScheduler = true)
class TriggerTest extends BaseSolaceIT {

    static final String TEST_QUEUE = "test";

    @Inject
    private DispatchQueueInterface<Execution> executionQueue;

    @Inject
    private TestRunnerUtils runnerUtils;

    @Inject
    private RunContextFactory runContextFactory;

    @Inject
    protected LocalFlowRepositoryLoader repositoryLoader;

    @Test
    void testTriggerTask() throws Exception {
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

        Execution lastExecution = runnerUtils.awaitFlowExecution(e -> true, MAIN_TENANT, "io.kestra.tests", "trigger", Duration.ofMinutes(1));
        assertThat(lastExecution, notNullValue());

        Integer trigger = (Integer) lastExecution.getTrigger().getVariables().get("messagesCount");

        assertThat(trigger, greaterThanOrEqualTo(2));
    }
}

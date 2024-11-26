package io.kestra.plugin.solace;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.ExecutionTrigger;
import io.kestra.core.models.flows.State;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.triggers.AbstractTrigger;
import io.kestra.core.models.triggers.PollingTriggerInterface;
import io.kestra.core.models.triggers.TriggerContext;
import io.kestra.core.models.triggers.TriggerOutput;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.solace.serde.Serdes;
import io.kestra.plugin.solace.service.receiver.QueueTypes;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The {@link Trigger} can be used for triggering flow based on messages received from Solace.
 */
@Plugin(examples = {
    @Example(
        title = "Trigger flow based on messages received from a Solace broker.",
        full = true,
        code = {
            """
                id: trigger_from_solace_queue
                namespace: company.team

                tasks:
                  - id: hello
                    type: io.kestra.plugin.core.log.Log
                    message: Hello there! I received {{ trigger.messagesCount }} from Solace!

                triggers:
                  - id: read_from_solace
                    type: io.kestra.plugin.solace.Trigger
                    interval: PT30S
                    host: localhost:55555
                    username: admin
                    password: admin
                    vpn: default
                    messageDeserializer: JSON
                    queueName: test_queue
                    queueType: DURABLE_EXCLUSIVE
                """
        }
    )
})
@Schema(
    title = "Trigger flow based on messages received from a Solace broker."
)
@SuperBuilder
@NoArgsConstructor
@Getter
public class Trigger extends AbstractTrigger implements SolaceConsumeInterface, PollingTriggerInterface, TriggerOutput<Consume.Output> {


    // TRIGGER'S PROPERTIES
    @Builder.Default
    private Duration interval = Duration.ofSeconds(60);

    // TASK'S PROPERTIES
    private Property<String> username;

    private Property<String> password;

    @Builder.Default
    private Property<String> vpn = Property.of("default");

    private Property<String> host;

    @Builder.Default
    private Property<Map<String, String>> properties = Property.of(new HashMap<>());

    private Property<String> queueName;

    private Property<QueueTypes> queueType;

    @Builder.Default
    private Property<Serdes> messageDeserializer = Property.of(Serdes.STRING);

    @Builder.Default
    private Property<Map<String, Object>> messageDeserializerProperties = Property.of(new HashMap<>());

    @Builder.Default
    private Property<Integer> maxMessages = Property.of(100);

    @Builder.Default
    private Property<Duration> maxDuration = Property.of(Duration.ofSeconds(10));

    private Property<String> messageSelector;

    /**
     * {@inheritDoc}
     **/
    @Override
    public Optional<Execution> evaluate(ConditionContext conditionContext,
                                        TriggerContext context) throws Exception {

        final RunContext runContext = conditionContext.getRunContext();
        final Logger logger = runContext.logger();

        Consume.Output output = new Consume().run(runContext, this);

        if (logger.isDebugEnabled()) {
            logger.debug("Received '{}' message from queue '{}'", output.getMessagesCount(), getQueueName());
        }

        if (output.getMessagesCount() == 0) {
            return Optional.empty();
        }

        ExecutionTrigger executionTrigger = ExecutionTrigger.of(this, output);
        Execution execution = Execution.builder()
            .id(runContext.getTriggerExecutionId())
            .namespace(context.getNamespace())
            .flowId(context.getFlowId())
            .flowRevision(conditionContext.getFlow().getRevision())
            .state(new State())
            .trigger(executionTrigger)
            .build();

        return Optional.of(execution);
    }
}

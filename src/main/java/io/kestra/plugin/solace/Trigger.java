package io.kestra.plugin.solace;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.conditions.ConditionContext;
import io.kestra.core.models.executions.Execution;
import io.kestra.core.models.executions.ExecutionTrigger;
import io.kestra.core.models.flows.State;
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
import java.util.Collections;
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
                id: TriggerFromSolaceQueue
                namespace: company.team
                tasks:
                  - id: hello
                    type: io.kestra.plugin.core.log.Log
                    message: Hello there! I received {{trigger.messagesCount}} from Solace!
                triggers:
                  - id: readFromSolace
                    type: "io.kestra.plugin.solace.Trigger"
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
    private String username;

    private String password;

    @Builder.Default
    private String vpn = "default";

    private String host;

    @Builder.Default
    private Map<String, String> properties = Collections.emptyMap();

    private String queueName;

    private QueueTypes queueType;

    @Builder.Default
    private Serdes messageDeserializer = Serdes.STRING;

    @Builder.Default
    private Map<String, Object> messageDeserializerProperties = Collections.emptyMap();

    @Builder.Default
    private Integer maxMessages = 100;

    @Builder.Default
    private Duration maxDuration = Duration.ofSeconds(10);

    private String messageSelector;

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

package io.kestra.plugin.solace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.solace.Service;
import org.testcontainers.solace.SolaceContainer;

import java.io.IOException;

@Testcontainers
public class BaseSolaceIT {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumeTest.class);

    static final String SOLACE_USER = "user";
    static final String SOLACE_PASSWORD = "pass";
    static final String SOLACE_VPN = "default";

    @Container
    SolaceContainer solaceContainer = new SolaceContainer("solace/solace-pubsub-standard:10.2") {
        {
            addFixedExposedPort(55555, 55555);
        }
    }
        .withCredentials(SOLACE_USER, SOLACE_PASSWORD)
        .withTopic("topic", Service.SMF)
        .withLogConsumer(new Slf4jLogConsumer(LOG))
        .withVpn(SOLACE_VPN);

    protected void createQueueWithSubscriptionTopic(String queueName,
                                                    String subscriptionTopic) {
        executeCommand("curl",
            "http://localhost:8080/SEMP/v2/config/msgVpns/" + SOLACE_VPN + "/topicEndpoints",
            "-X", "POST",
            "-u", "admin:admin",
            "-H", "Content-Type:application/json",
            "-d", "{\"topicEndpointName\":\"" + subscriptionTopic + "\",\"accessType\":\"exclusive\",\"permission\":\"modify-topic\",\"ingressEnabled\":true,\"egressEnabled\":true}"
        );
        executeCommand("curl",
            "http://localhost:8080/SEMP/v2/config/msgVpns/" + SOLACE_VPN + "/queues",
            "-X", "POST",
            "-u", "admin:admin",
            "-H", "Content-Type:application/json",
            "-d", "{\"queueName\":\"" + queueName + "\",\"accessType\":\"exclusive\",\"maxMsgSpoolUsage\":200,\"permission\":\"consume\",\"ingressEnabled\":true,\"egressEnabled\":true}"
        );
        executeCommand("curl",
            "http://localhost:8080/SEMP/v2/config/msgVpns/" + SOLACE_VPN + "/queues/" + queueName + "/subscriptions",
            "-X", "POST",
            "-u", "admin:admin",
            "-H", "Content-Type:application/json",
            "-d", "{\"subscriptionTopic\":\"" + subscriptionTopic + "\"}"
        );
    }

    protected void executeCommand(String... command) {
        try {
            org.testcontainers.containers.Container.ExecResult execResult = solaceContainer.execInContainer(command);
            if (execResult.getExitCode() != 0) {
                logCommandError(execResult.getStderr(), command);
            } else {
                LOG.info(execResult.getStdout());
            }
        } catch (IOException | InterruptedException e) {
            logCommandError(e.getMessage(), command);
        }
    }

    protected void logCommandError(String error, String... command) {
        LOG.error("Could not execute command {}: {}", command, error);
    }
}

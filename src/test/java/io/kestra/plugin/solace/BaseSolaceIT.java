package io.kestra.plugin.solace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;

@Testcontainers
public class BaseSolaceIT {

    private static final Logger LOG = LoggerFactory.getLogger(ConsumeTest.class);

    static final String SOLACE_USER = "user";
    static final String SOLACE_PASSWORD = "pass";
    static final String SOLACE_VPN = "default";

    @Container
    GenericContainer<?> solaceContainer = new GenericContainer<>(
        DockerImageName.parse("solace/solace-pubsub-standard:10.2"))
        .withEnv("username_admin_globalaccesslevel", "admin")
        .withEnv("username_admin_password", "admin")
        .withEnv("username_" + SOLACE_USER + "_globalaccesslevel", "user")
        .withEnv("username_" + SOLACE_USER + "_password", SOLACE_PASSWORD)
        .withEnv("system_scaling_maxconnectioncount", "100")
        .withSharedMemorySize(1_000_000_000L)
        .withExposedPorts(55555, 8080, 1883, 8008, 9000)
        .withExposedPorts(55555, 55555)
        .withLogConsumer(new Slf4jLogConsumer(LOG))
        .waitingFor(Wait.forLogMessage(".*Running pre-startup checks.*", 1)
            .withStartupTimeout(Duration.ofMinutes(2)));

    protected String getUsername() {
        return SOLACE_USER;
    }

    protected String getPassword() {
        return SOLACE_PASSWORD;
    }

    protected String getVpn() {
        return SOLACE_VPN;
    }

    protected String getOrigin(Object service) {
        return solaceContainer.getHost() + ":" + solaceContainer.getMappedPort(55555);
    }

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

    public enum Service {
        SMF
    }
}
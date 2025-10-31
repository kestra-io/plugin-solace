package io.kestra.plugin.solace;

import com.github.dockerjava.api.model.Capability;
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
public abstract class BaseSolaceIT {

    private static final Logger LOG = LoggerFactory.getLogger(BaseSolaceIT.class);

    protected static final String SOLACE_USER = "user";
    protected static final String SOLACE_PASSWORD = "pass";
    protected static final String SOLACE_VPN = "default";

    protected static final int SMF_PORT = 55555;
    protected static final int SEMP_PORT = 8080;

    @Container
    protected GenericContainer<?> solaceContainer = new GenericContainer<>(
        DockerImageName.parse("solace/solace-pubsub-standard:10.2"))
        .withEnv("username_admin_globalaccesslevel", "admin")
        .withEnv("username_admin_password", "admin")
        .withEnv("username_" + SOLACE_USER + "_globalaccesslevel", "user")
        .withEnv("username_" + SOLACE_USER + "_password", SOLACE_PASSWORD)
        .withEnv("system_scaling_maxconnectioncount", "100")
        .withExposedPorts(SMF_PORT, SEMP_PORT)
        .withSharedMemorySize(2L * 1024 * 1024 * 1024) // 2GB
        .withCreateContainerCmdModifier(cmd ->
            cmd.getHostConfig().withCapAdd(Capability.IPC_LOCK))
        // Wait for broker to be fully up
        .waitingFor(
            Wait.forLogMessage(".*Primary Virtual Router is AD-Active.*", 1)
                .withStartupTimeout(Duration.ofMinutes(10))
        )
        .withLogConsumer(new Slf4jLogConsumer(LOG))
        .withReuse(true);

    /**
     * Returns the host and mapped port for SMF connections (e.g. tcp://localhost:55555)
     */
    protected String getOrigin(Service service) {
        return switch (service) {
            case SMF -> String.format("tcp://%s:%d",
                solaceContainer.getHost(),
                solaceContainer.getMappedPort(SMF_PORT));
        };
    }

    /**
     * Creates a queue and subscribes it to a topic via SEMPv2 inside the container.
     */
    protected void createQueueWithSubscriptionTopic(String queueName, String subscriptionTopic) {
        String sempBaseUrl = String.format(
            "http://%s:%d/SEMP/v2/config/msgVpns/%s",
            solaceContainer.getHost(),
            solaceContainer.getMappedPort(SEMP_PORT),
            SOLACE_VPN
        );

        // Create topic endpoint
        executeCommand("curl", "-s", "-X", "POST",
            "-u", "admin:admin",
            "-H", "Content-Type:application/json",
            "-d", "{\"topicEndpointName\":\"" + subscriptionTopic + "\",\"accessType\":\"exclusive\",\"permission\":\"modify-topic\",\"ingressEnabled\":true,\"egressEnabled\":true}",
            sempBaseUrl + "/topicEndpoints"
        );

        // Create queue
        executeCommand("curl", "-s", "-X", "POST",
            "-u", "admin:admin",
            "-H", "Content-Type:application/json",
            "-d", "{\"queueName\":\"" + queueName + "\",\"accessType\":\"exclusive\",\"maxMsgSpoolUsage\":200,\"permission\":\"consume\",\"ingressEnabled\":true,\"egressEnabled\":true}",
            sempBaseUrl + "/queues"
        );

        // Add subscription
        executeCommand("curl", "-s", "-X", "POST",
            "-u", "admin:admin",
            "-H", "Content-Type:application/json",
            "-d", "{\"subscriptionTopic\":\"" + subscriptionTopic + "\"}",
            sempBaseUrl + "/queues/" + queueName + "/subscriptions"
        );
    }

    /**
     * Execute a command inside the container and log output or errors.
     */
    protected void executeCommand(String... command) {
        try {
            var execResult = solaceContainer.execInContainer(command);
            if (execResult.getExitCode() != 0) {
                LOG.error("Command '{}' failed: {}", String.join(" ", command), execResult.getStderr());
            } else {
                LOG.info("Command '{}' succeeded: {}", String.join(" ", command), execResult.getStdout());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("Error executing command '{}': {}", String.join(" ", command), e.getMessage());
        }
    }

    public enum Service {
        SMF
    }
}

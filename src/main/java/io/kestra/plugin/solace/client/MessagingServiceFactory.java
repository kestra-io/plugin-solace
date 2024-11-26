package io.kestra.plugin.solace.client;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.solace.SolaceConnectionInterface;

import java.util.Objects;
import java.util.Properties;

public final class MessagingServiceFactory {

    public static MessagingService create(final SolaceConnectionInterface config, RunContext runContext) throws Exception {
        Objects.requireNonNull(config, "Cannot create new MessagingService with null configuration.");

        Properties properties = new Properties();
        properties.setProperty(SolaceProperties.TransportLayerProperties.HOST, runContext.render(config.getHost()).as(String.class).orElseThrow());
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, runContext.render(config.getVpn()).as(String.class).orElseThrow());

        runContext.render(config.getUsername()).as(String.class)
            .ifPresent(val -> properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_USER_NAME, val));

        runContext.render(config.getPassword()).as(String.class)
            .ifPresent(val -> properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_PASSWORD, val));

        var renderedProperties = runContext.render(config.getProperties()).asMap(String.class, String.class);
        if (!renderedProperties.isEmpty()) {
            renderedProperties.forEach(properties::setProperty);
        }

        return MessagingService
            .builder(ConfigurationProfile.V1)
            .fromProperties(properties)
            .build()
            .connect(); // this will block
    }
}

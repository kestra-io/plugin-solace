package io.kestra.plugin.solace.client;

import com.solace.messaging.MessagingService;
import com.solace.messaging.config.SolaceProperties;
import com.solace.messaging.config.profile.ConfigurationProfile;
import io.kestra.plugin.solace.SolaceConnectionInterface;

import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

public final class MessagingServiceFactory {

    public static MessagingService create(final SolaceConnectionInterface config) throws Exception {
        Objects.requireNonNull(config, "Cannot create new MessagingService with null configuration.");

        Properties properties = new Properties();
        properties.setProperty(SolaceProperties.TransportLayerProperties.HOST, config.getHost());
        properties.setProperty(SolaceProperties.ServiceProperties.VPN_NAME, config.getVpn());

        Optional.ofNullable(config.getUsername())
            .ifPresent(val -> properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_USER_NAME, val));

        Optional.ofNullable(config.getPassword())
            .ifPresent(val -> properties.setProperty(SolaceProperties.AuthenticationProperties.SCHEME_BASIC_PASSWORD, val));

        if (config.getProperties() != null) {
            config.getProperties().forEach(properties::setProperty);
        }

        return MessagingService
            .builder(ConfigurationProfile.V1)
            .fromProperties(properties)
            .build()
            .connect(); // this will block
    }
}

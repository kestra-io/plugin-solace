package io.kestra.plugin.solace.service.publisher;

import java.util.Map;
import java.util.Optional;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.SDTMap;
import com.solacesystems.jcsmp.XMLMessageProducer;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;

import io.kestra.plugin.solace.serde.Serde;
import io.kestra.core.serializers.FileSerde;
import io.kestra.plugin.solace.service.publisher.AbstractSolaceDirectMessagePublisher.OutboundMessageObject;

public final class SolaceQueueMessagePublisher {

    private final String host;
    private final String vpn;
    private final String username;
    private final String password;
    private final String queueName;
    private final Serde serde;
    private final Map<String, String> connectionProperties;

    public SolaceQueueMessagePublisher(
        String host,
        String vpn,
        String username,
        String password,
        String queueName,
        Serde serde,
        Map<String, String> connectionProperties
    ) {
        this.host = host;
        this.vpn = vpn;
        this.username = username;
        this.password = password;
        this.queueName = queueName;
        this.serde = serde;
        this.connectionProperties = connectionProperties;
    }

    public int send(
        InputStream inputStream,
        Map<String, String> messageProperties
    ) throws Exception {
        JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, host);
        properties.setProperty(JCSMPProperties.VPN_NAME, vpn);
        properties.setProperty(JCSMPProperties.USERNAME, username);
        properties.setProperty(JCSMPProperties.PASSWORD, password);

        if (connectionProperties != null) {
            connectionProperties.forEach(properties::setProperty);
        }

        JCSMPSession session = null;
        XMLMessageProducer producer = null;

        try {
            session = JCSMPFactory.onlyInstance().createSession(properties);
            session.connect();

            AtomicReference<CompletableFuture<Void>> pendingSend = new AtomicReference<>();

            producer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
                @Override
                public void responseReceived(String messageID) {
                    CompletableFuture<Void> future = pendingSend.get();
                    if (future != null) {
                        future.complete(null);
                    }
                }

                @Override
                public void handleError(String messageID, JCSMPException e, long timestamp) {
                    CompletableFuture<Void> future = pendingSend.get();
                    if (future != null) {
                        future.completeExceptionally(e);
                    }
                }
            });

            Destination queue = JCSMPFactory.onlyInstance().createQueue(queueName);

            int totalSentMessages = 0;

            for (OutboundMessageObject object : FileSerde.readAll(inputStream, OutboundMessageObject.class).toIterable()) {
                byte[] payloadAsBytes = Optional.ofNullable(object.payload())
                    .map(serde::serialize)
                    .orElseGet(() -> new byte[0]);

                BytesXMLMessage message = JCSMPFactory.onlyInstance().createBytesXMLMessage();
                message.setDeliveryMode(com.solacesystems.jcsmp.DeliveryMode.PERSISTENT);
                message.writeBytes(payloadAsBytes);

                if (messageProperties != null || object.properties() != null) {
                    SDTMap propertiesMap = JCSMPFactory.onlyInstance().createMap();

                    if (messageProperties != null) {
                        for (Map.Entry<String, String> entry : messageProperties.entrySet()) {
                            propertiesMap.putString(entry.getKey(), entry.getValue());
                        }
                    }

                    if (object.properties() != null) {
                        for (Map.Entry<String, String> entry : object.properties().entrySet()) {
                            propertiesMap.putString(entry.getKey(), entry.getValue());
                        }
                    }

                    message.setProperties(propertiesMap);
                }

                CompletableFuture<Void> sendFuture = new CompletableFuture<>();
                pendingSend.set(sendFuture);

                producer.send(message, queue);

                sendFuture.get(10, TimeUnit.SECONDS);
                pendingSend.set(null);

                totalSentMessages++;
            }

            return totalSentMessages;
        } finally {
            if (producer != null) {
                producer.close();
            }
            if (session != null) {
                session.closeSession();
            }
        }
    }
}
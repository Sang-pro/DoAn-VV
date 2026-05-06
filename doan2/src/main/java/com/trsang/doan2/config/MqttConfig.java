package com.trsang.doan2.config;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.annotation.IntegrationComponentScan;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@IntegrationComponentScan(basePackages = "com.trsang.doan2.services.interfaces")
public class MqttConfig {

    @Value("${mqtt.server-uris:ssl://localhost:8883}")
    private String mqttServerUris;

    @Value("${mqtt.username:}")
    private String mqttUsername;

    @Value("${mqtt.password:}")
    private String mqttPassword;

    @Value("${mqtt.client-id:springBootClient}")
    private String mqttClientId;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // Parse server URIs (comma-separated)
        if (mqttServerUris != null && !mqttServerUris.isEmpty()) {
            String[] serverUris = mqttServerUris.split(",");
            options.setServerURIs(serverUris);
            log.info("Configured MQTT server URIs: {}", String.join(", ", serverUris));
        } else {
            log.warn("MQTT server URIs not configured, using default: ssl://localhost:8883");
            options.setServerURIs(new String[]{"ssl://localhost:8883"});
        }

        // Set authentication if credentials provided
        if (mqttUsername != null && !mqttUsername.isEmpty()) {
            options.setUserName(mqttUsername);
            log.info("MQTT username configured: {}", mqttUsername);
        }
        if (mqttPassword != null && !mqttPassword.isEmpty()) {
            options.setPassword(mqttPassword.toCharArray());
            log.info("MQTT password configured");
        }

        // SSL/TLS Configuration for HiveMQ Cloud (Chỉ bật nếu dùng ssl://)
        if (mqttServerUris != null && mqttServerUris.contains("ssl://")) {
            try {
                // Create a trust manager that accepts all certificates
                javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                    new javax.net.ssl.X509TrustManager() {
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new java.security.cert.X509Certificate[0];
                        }
                        public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                        }
                        public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                        }
                    }
                };

                // Create SSL context with the trust manager
                javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                
                options.setSocketFactory(sslContext.getSocketFactory());
                log.info("SSL/TLS socket factory configured for MQTT connection");
            } catch (Exception e) {
                log.error("Failed to configure SSL/TLS for MQTT: {}", e.getMessage(), e);
            }
        }

        // Connection options
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);
        options.setKeepAliveInterval(60);

        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        // Use the first configured server URI (HiveMQ Cloud)
        String serverUri = mqttServerUris.split(",")[0];
        log.info("Configuring MQTT inbound adapter with broker: {}", serverUri);
        log.info("Subscribing to topic: smarttrash/+/data and esl/#");
        
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        serverUri,
                        mqttClientId + "_in",
                        mqttClientFactory(),
                        "smarttrash/+/data", "esl/#");

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(1);
        adapter.setOutputChannel(mqttInputChannel());

        return adapter;
    }

    @Bean
    public MqttClient mqttClient(MqttPahoClientFactory factory) throws MqttException {
        MqttClient client = new MqttClient(
            mqttServerUris.split(",")[0], 
            mqttClientId, 
            null
        );
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost: {}", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                log.debug("Message arrived on topic {}: {}", topic, new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                log.debug("Message delivery completed");
            }
        });
        
        return client;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(mqttClientId + "_out", mqttClientFactory());
        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic("esl/default");
        return messageHandler;
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }
}

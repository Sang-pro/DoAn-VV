package com.trsang.doan2.config;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
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

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
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
        } else {
            log.warn("MQTT server URIs not configured, using default: ssl://localhost:8883");
            options.setServerURIs(new String[]{"ssl://localhost:8883"});
        }

        // Set authentication if credentials provided
        if (mqttUsername != null && !mqttUsername.isEmpty()) {
            options.setUserName(mqttUsername);
        }
        if (mqttPassword != null && !mqttPassword.isEmpty()) {
            options.setPassword(mqttPassword.toCharArray());
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
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(
                        "tcp://localhost:1883",
                        mqttClientId,
                        mqttClientFactory(),
                        "data/+/sensors");

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
        client.setCallback(new org.eclipse.paho.client.mqttv3.MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("MQTT connection lost: {}", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, org.eclipse.paho.client.mqttv3.MqttMessage message) throws Exception {
                log.debug("Message arrived on topic {}: {}", topic, new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken token) {
                log.debug("Message delivery completed");
            }
        });
        
        return client;
    }
}

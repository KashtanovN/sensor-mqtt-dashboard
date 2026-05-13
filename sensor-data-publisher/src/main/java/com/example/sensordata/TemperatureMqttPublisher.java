package com.example.sensordata;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Component
@ConditionalOnProperty(name = "sensor.publish.enabled", havingValue = "true", matchIfMissing = true)
public class TemperatureMqttPublisher {

    private static final Logger log = LoggerFactory.getLogger(TemperatureMqttPublisher.class);

    private final SensorPublishProperties properties;

    private MqttClient client;

    public TemperatureMqttPublisher(SensorPublishProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void connect() {
        try {
            var mqtt = properties.getMqtt();
            String clientId = mqtt.getClientId() + "-" + UUID.randomUUID();
            client = new MqttClient(mqtt.getBrokerUrl(), clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            if (mqtt.getUsername() != null && !mqtt.getUsername().isBlank()) {
                options.setUserName(mqtt.getUsername());
            }
            if (mqtt.getPassword() != null && !mqtt.getPassword().isBlank()) {
                options.setPassword(mqtt.getPassword().toCharArray());
            }

            client.connect(options);
            log.info("Publisher connected to {} as {}", mqtt.getBrokerUrl(), clientId);
        } catch (MqttException e) {
            throw new IllegalStateException("Failed to connect MQTT publisher", e);
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception ignored) {
            // ignore
        }
        try {
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    @Scheduled(fixedRateString = "${sensor.publish.interval-ms:600000}")
    public void publish() {
        try {
            if (client == null || !client.isConnected()) {
                log.warn("MQTT client is not connected; skipping publish");
                return;
            }

            double min = properties.getPublish().getTemperatureMin();
            double max = properties.getPublish().getTemperatureMax();
            double temperature = ThreadLocalRandom.current().nextDouble(min, max);

            String json = String.format(Locale.ROOT, "{\"temperature\":%s}", Double.toString(temperature));
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            MqttMessage message = new MqttMessage(bytes);
            message.setQos(1);

            String topic = properties.getMqtt().getTopic();
            client.publish(topic, message);
            log.info("Published temperature {} °C to topic {}", temperature, topic);
        } catch (Exception e) {
            log.error("MQTT publish failed", e);
        }
    }
}

package com.company.userregistration.integration.mqtt;

import com.company.userregistration.entity.Sensor;
import com.company.userregistration.entity.SensorReading;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttSensorIngestService implements MqttCallback {

    private static final Logger log = LoggerFactory.getLogger(MqttSensorIngestService.class);

    private final MqttProperties mqttProperties;
    private final DataManager dataManager;
    private final SystemAuthenticator systemAuthenticator;
    private final ObjectMapper objectMapper;

    private final Object clientLock = new Object();
    private MqttClient client;
    private final Map<String, UUID> topicToSensorId = new ConcurrentHashMap<>();
    private final Set<String> subscribedTopics = ConcurrentHashMap.newKeySet();

    public MqttSensorIngestService(MqttProperties mqttProperties,
                                   DataManager dataManager,
                                   SystemAuthenticator systemAuthenticator,
                                   ObjectMapper objectMapper) {
        this.mqttProperties = mqttProperties;
        this.dataManager = dataManager;
        this.systemAuthenticator = systemAuthenticator;
        this.objectMapper = objectMapper;
    }

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        if (event.getApplicationContext().getParent() != null) {
            return;
        }
        refreshSubscriptions();
    }

    @Scheduled(fixedDelayString = "${mqtt.subscription-refresh-ms:30000}")
    public void scheduledRefresh() {
        refreshSubscriptions();
    }

    public void refreshSubscriptions() {
        synchronized (clientLock) {
            try {
                List<Sensor> sensors = new ArrayList<>();
                systemAuthenticator.runWithSystem(() ->
                        sensors.addAll(dataManager.load(Sensor.class).all().list())
                );

                Map<String, UUID> desired = new HashMap<>();
                for (Sensor sensor : sensors) {
                    if (sensor.getMqttTopic() == null || sensor.getMqttTopic().isBlank()) {
                        continue;
                    }
                    desired.put(sensor.getMqttTopic(), sensor.getId());
                }

                topicToSensorId.clear();
                topicToSensorId.putAll(desired);

                if (desired.isEmpty()) {
                    subscribedTopics.clear();
                    disconnectQuietly();
                    return;
                }

                ensureConnected();
                if (client == null || !client.isConnected()) {
                    return;
                }

                Set<String> desiredTopicSet = desired.keySet();

                for (String topic : new HashSet<>(subscribedTopics)) {
                    if (!desiredTopicSet.contains(topic)) {
                        client.unsubscribe(topic);
                        subscribedTopics.remove(topic);
                    }
                }

                for (String topic : desiredTopicSet) {
                    if (!subscribedTopics.contains(topic)) {
                        client.subscribe(topic, 1);
                        subscribedTopics.add(topic);
                        log.info("Subscribed to MQTT topic {}", topic);
                    }
                }
            } catch (Exception e) {
                log.warn("MQTT subscription refresh failed: {}", e.toString());
            }
        }
    }

    private void ensureConnected() throws MqttException {
        if (client != null && client.isConnected()) {
            return;
        }

        disconnectQuietly();

        String clientId = mqttProperties.getClientId() + "-" + UUID.randomUUID();
        client = new MqttClient(mqttProperties.getBrokerUrl(), clientId, new MemoryPersistence());
        client.setCallback(this);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(mqttProperties.getConnectionTimeoutSeconds());
        options.setKeepAliveInterval(mqttProperties.getKeepAliveIntervalSeconds());

        if (mqttProperties.getUsername() != null && !mqttProperties.getUsername().isBlank()) {
            options.setUserName(mqttProperties.getUsername());
        }
        if (mqttProperties.getPassword() != null && !mqttProperties.getPassword().isBlank()) {
            options.setPassword(mqttProperties.getPassword().toCharArray());
        }

        client.connect(options);
        log.info("Connected to MQTT broker {}", mqttProperties.getBrokerUrl());
    }

    private void disconnectQuietly() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (Exception ignored) {
            // ignore
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } catch (Exception ignored) {
                // ignore
            } finally {
                client = null;
                subscribedTopics.clear();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        synchronized (clientLock) {
            disconnectQuietly();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("MQTT connection lost: {}", cause == null ? "unknown" : cause.toString());
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        UUID sensorId = topicToSensorId.get(topic);
        if (sensorId == null) {
            return;
        }

        final String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        final double temperature;
        try {
            temperature = parseTemperature(payload);
        } catch (Exception e) {
            log.warn("Unsupported MQTT payload on {}: {}", topic, payload);
            return;
        }

        systemAuthenticator.runWithSystem(() -> {
            Sensor sensor = dataManager.load(Sensor.class).id(sensorId).optional().orElse(null);
            if (sensor == null) {
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            SensorReading reading = dataManager.create(SensorReading.class);
            reading.setSensor(sensor);
            reading.setMeasuredAt(now);
            reading.setTemperature(temperature);

            sensor.setLastTemperature(temperature);
            sensor.setLastRecordedAt(now);

            SaveContext saveContext = new SaveContext();
            saveContext.saving(reading);
            saveContext.saving(sensor);
            dataManager.save(saveContext);
        });
    }

    private double parseTemperature(String payload) throws Exception {
        String trimmed = payload.trim();
        try {
            return Double.parseDouble(trimmed);
        } catch (NumberFormatException ignored) {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.isObject() && node.has("temperature")) {
                return node.get("temperature").asDouble();
            }
            if (node.isNumber()) {
                return node.asDouble();
            }
            throw new IllegalArgumentException("Payload is not a number or JSON with temperature");
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // not used
    }
}

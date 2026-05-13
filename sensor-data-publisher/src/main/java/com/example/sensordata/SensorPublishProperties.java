package com.example.sensordata;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sensor")
public class SensorPublishProperties {

    private final Mqtt mqtt = new Mqtt();
    private final Publish publish = new Publish();

    public Mqtt getMqtt() {
        return mqtt;
    }

    public Publish getPublish() {
        return publish;
    }

    public static class Mqtt {
        private String brokerUrl = "tcp://127.0.0.1:1883";
        private String username = "";
        private String password = "";
        private String clientId = "sensor-data-publisher";
        private String topic = "sensors/demo/temperature";

        public String getBrokerUrl() {
            return brokerUrl;
        }

        public void setBrokerUrl(String brokerUrl) {
            this.brokerUrl = brokerUrl;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getTopic() {
            return topic;
        }

        public void setTopic(String topic) {
            this.topic = topic;
        }
    }

    public static class Publish {
        private boolean enabled = true;
        private long intervalMs = 600_000L;
        private double temperatureMin = 20.0;
        private double temperatureMax = 60.0;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getIntervalMs() {
            return intervalMs;
        }

        public void setIntervalMs(long intervalMs) {
            this.intervalMs = intervalMs;
        }

        public double getTemperatureMin() {
            return temperatureMin;
        }

        public void setTemperatureMin(double temperatureMin) {
            this.temperatureMin = temperatureMin;
        }

        public double getTemperatureMax() {
            return temperatureMax;
        }

        public void setTemperatureMax(double temperatureMax) {
            this.temperatureMax = temperatureMax;
        }
    }
}

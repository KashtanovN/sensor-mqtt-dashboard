package com.company.userregistration.integration.mqtt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    /**
     * Broker URL, for example {@code tcp://127.0.0.1:1883} or {@code ssl://...} for TLS endpoints.
     */
    private String brokerUrl = "tcp://127.0.0.1:1883";

    private String username = "";
    private String password = "";

    private String clientId = "sensor-data-visualizer";

    private int connectionTimeoutSeconds = 10;

    private int keepAliveIntervalSeconds = 60;

    private long subscriptionRefreshMs = 30_000L;

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

    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public int getKeepAliveIntervalSeconds() {
        return keepAliveIntervalSeconds;
    }

    public void setKeepAliveIntervalSeconds(int keepAliveIntervalSeconds) {
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
    }

    public long getSubscriptionRefreshMs() {
        return subscriptionRefreshMs;
    }

    public void setSubscriptionRefreshMs(long subscriptionRefreshMs) {
        this.subscriptionRefreshMs = subscriptionRefreshMs;
    }
}

package com.example.sensordata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(SensorPublishProperties.class)
public class SensorDataPublisherApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorDataPublisherApplication.class, args);
    }
}

# sensor-data-publisher

Spring Boot service that connects to an MQTT broker and publishes JSON payloads shaped as:

```json
{"temperature": 42.7}
```

## Configuration

See `src/main/resources/application.yml`. Important keys:

| Key | Meaning |
| --- | --- |
| `sensor.mqtt.broker-url` | Broker URL (`tcp://…` or `ssl://…`) |
| `sensor.mqtt.topic` | Topic name (must match a `Sensor.mqttTopic` value in the Jmix app) |
| `sensor.publish.interval-ms` | Publish interval (default `600000` = 10 minutes) |
| `sensor.publish.temperature-min` / `temperature-max` | Random range in °C |

## Run

```bash
./gradlew bootRun
```

## Yandex IoT Core (optional)

If you use a vendor MQTT endpoint, set `sensor.mqtt.broker-url`, `sensor.mqtt.username`, and `sensor.mqtt.password` according to that provider’s Java/MQTT documentation (for example TLS `ssl://` URLs and registry credentials). The message format stays the same.

# sensor-data-visualizer

Jmix **2.8** (Flow UI) application that:

- stores `Sensor` rows, each with its own **MQTT topic** to subscribe to;
- persists every incoming reading as `SensorReading`;
- shows a **sensor list** with last temperature and a **line chart** of all stored readings for a sensor.

Default login: **admin / admin** (from `application.properties`).

## MQTT

Configure `mqtt.*` keys in `src/main/resources/application.properties`. The subscriber refreshes topic subscriptions periodically (`mqtt.subscription-refresh-ms`) so new or edited sensors are picked up without restarting.

The broker can be any MQTT 3.1.1 endpoint. For Yandex IoT Core or other vendors, point `mqtt.broker-url` at their `ssl://` endpoint and set credentials as required by their documentation.

## Run

```bash
./gradlew bootRun
```

Then open `http://localhost:8080`.

## Notes

- The project was bootstrapped from the Jmix `user-registration` sample; user self-registration views remain in the codebase but are hidden from the login form for this assignment-focused flow.
- Charts require the Jmix Charts add-on (`jmix-charts-flowui-starter`), already declared in `build.gradle`.

# IoT MQTT demo workspace

This repository contains two services that match the coursework-style specification:

- `sensor-data-publisher` — Spring Boot job that publishes random temperatures (20–60 °C) to a fixed MQTT topic on a schedule (default **10 minutes**).
- `sensor-data-visualizer` — Jmix (Flow UI) application that subscribes to MQTT topics configured per `Sensor` row, stores readings in an embedded HSQLDB database, and shows a list plus a line chart.

The MQTT broker is **not** tied to a specific vendor: anything compatible with **MQTT 3.1.1** works (local Mosquitto, a private broker, or a cloud IoT MQTT endpoint such as Yandex IoT Core with TLS and credentials configured in `application.properties` / `application.yml`). Wi-Fi is only the transport from a physical device to your network; this demo uses software publishers and a TCP connection to the broker.

## Quick start (local Mosquitto)

1. Start the broker:

```bash
docker compose up -d
```

2. Start the publisher (from `sensor-data-publisher`):

```bash
./gradlew bootRun
```

For a quicker demo you can temporarily lower `sensor.publish.interval-ms` in `sensor-data-publisher/src/main/resources/application.yml`.

3. Start the visualizer (from `sensor-data-visualizer`):

```bash
./gradlew bootRun
```

4. Open `http://localhost:8080`, log in as **admin / admin**, open **Sensors**, select the demo row, click **Temperature chart**, and use **Refresh** after new MQTT messages arrive.

## Demo video and GitHub

- **Video:** record a short screen capture showing the broker (optional), publisher logs, Jmix UI with the chart updating, and upload it to your platform of choice (for example an unlisted YouTube video or a file in the repo release assets).
- **GitHub:** create a public repository, push this workspace, and add the two per-project `README.md` files as included here.

## Requirements

- JDK **17+** and a network connection for the first Gradle download.

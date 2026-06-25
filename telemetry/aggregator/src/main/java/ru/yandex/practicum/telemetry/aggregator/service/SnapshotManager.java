package ru.yandex.practicum.telemetry.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class SnapshotManager {

    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        if (event == null) {
            return Optional.empty();
        }

        String hubId = event.getHubId();
        String sensorId = event.getId();
        Instant eventTimestamp = event.getTimestamp();

        if (hubId == null || sensorId == null || eventTimestamp == null) {
            return Optional.empty();
        }

        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(hubId, id ->
                SensorsSnapshotAvro.newBuilder()
                        .setHubId(id)
                        .setTimestamp(eventTimestamp)
                        .setSensorsState(new HashMap<>())
                        .build()
        );

        Map<String, SensorStateAvro> sensorsState = snapshot.getSensorsState();
        SensorStateAvro oldState = sensorsState.get(sensorId);

        if (oldState != null) {
            Instant oldTimestamp = oldState.getTimestamp();

            if (!eventTimestamp.isAfter(oldTimestamp)) {
                return Optional.empty();
            }

            Object oldData = oldState.getData();
            Object newData = event.getPayload();
            if (oldData != null && oldData.equals(newData)) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventTimestamp)
                .setData(event.getPayload())
                .build();

        sensorsState.put(sensorId, newState);
        snapshot.setTimestamp(eventTimestamp);

        log.info("Обновлен снапшот для хаба {}. Датчик {} обновлен", hubId, sensorId);
        return Optional.of(snapshot);
    }

    public Optional<SensorsSnapshotAvro> getSnapshot(String hubId) {
        return Optional.ofNullable(snapshots.get(hubId));
    }

    public int getSnapshotsCount() {
        return snapshots.size();
    }
}
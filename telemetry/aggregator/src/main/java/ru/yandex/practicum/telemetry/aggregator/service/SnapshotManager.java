package ru.yandex.practicum.telemetry.aggregator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
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

    public Optional<SensorsSnapshotAvro> updateState(@NonNull SensorEventAvro event) {
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

        SensorStateAvro oldState = snapshot.getSensorsState().get(sensorId);

        if (oldState != null) {
            if (oldState.getTimestamp().isAfter(eventTimestamp)
                    || oldState.getData().equals(event.getPayload())) {
                return Optional.empty();
            }
        }

        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(eventTimestamp)
                .setData(event.getPayload())
                .build();

        snapshot.getSensorsState().put(sensorId, newState);
        snapshot.setTimestamp(eventTimestamp);
        return Optional.of(snapshot);
    }

    public Optional<SensorsSnapshotAvro> getSnapshot(String hubId) {
        return Optional.ofNullable(snapshots.get(hubId));
    }
}
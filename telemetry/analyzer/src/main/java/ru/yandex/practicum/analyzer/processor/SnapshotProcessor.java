package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.model.Action;
import ru.yandex.practicum.analyzer.service.GrpcClientService;
import ru.yandex.practicum.analyzer.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> snapshotConsumer;
    private final ScenarioService scenarioService;
    private final GrpcClientService grpcClientService;
    private volatile boolean running = true;

    public void start() {
        try {
            snapshotConsumer.subscribe(Collections.singletonList("telemetry.snapshots.v1"));
            log.info("SnapshotProcessor subscribed to telemetry.snapshots.v1");

            while (running) {
                ConsumerRecords<String, SensorsSnapshotAvro> records =
                        snapshotConsumer.poll(Duration.ofMillis(5000));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    processSnapshot(record.value());
                }

                snapshotConsumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Error in SnapshotProcessor", e);
        } finally {
            snapshotConsumer.close();
            log.info("SnapshotProcessor consumer closed");
        }
    }

    private void processSnapshot(SensorsSnapshotAvro snapshot) {
        try {
            String hubId = snapshot.getHubId();
            log.debug("Processing snapshot for hub: {}", hubId);

            Map<String, Action> actions = scenarioService.analyzeSnapshot(snapshot);

            if (!actions.isEmpty()) {
                log.info("Found {} actions to execute for hub {}", actions.size(), hubId);
                actions.forEach((sensorId, action) ->
                        grpcClientService.sendAction(hubId, "scenario", action));
            }
        } catch (Exception e) {
            log.error("Error processing snapshot for hub: {}", snapshot.getHubId(), e);
        }
    }

    public void shutdown() {
        running = false;
    }
}
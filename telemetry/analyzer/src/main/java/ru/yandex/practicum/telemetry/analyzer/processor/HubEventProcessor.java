package ru.yandex.practicum.telemetry.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.service.ScenarioService;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class HubEventProcessor implements Runnable {

    private final KafkaConsumer<String, HubEventAvro> hubEventConsumer;
    private final ScenarioService scenarioService;
    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            hubEventConsumer.subscribe(Collections.singletonList("telemetry.hubs.v1"));
            log.info("HubEventProcessor subscribed to telemetry.hubs.v1");

            while (running) {
                ConsumerRecords<String, HubEventAvro> records =
                        hubEventConsumer.poll(Duration.ofMillis(5000));

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    processHubEvent(record.value());
                }

                hubEventConsumer.commitSync();
            }
        } catch (Exception e) {
            log.error("Error in HubEventProcessor", e);
        } finally {
            hubEventConsumer.close();
            log.info("HubEventProcessor consumer closed");
        }
    }

    private void processHubEvent(HubEventAvro event) {
        try {
            Object payload = event.getPayload();

            if (payload instanceof DeviceAddedEventAvro) {
                DeviceAddedEventAvro deviceAdded = (DeviceAddedEventAvro) payload;
                Sensor sensor = Sensor.builder()
                        .id(deviceAdded.getId())
                        .hubId(event.getHubId())
                        .build();
                scenarioService.addSensor(sensor);

            } else if (payload instanceof DeviceRemovedEventAvro) {
                DeviceRemovedEventAvro deviceRemoved = (DeviceRemovedEventAvro) payload;
                scenarioService.removeSensor(event.getHubId(), deviceRemoved.getId());

            } else if (payload instanceof ScenarioAddedEventAvro) {
                ScenarioAddedEventAvro scenarioAdded = (ScenarioAddedEventAvro) payload;
                convertAndSaveScenario(event.getHubId(), scenarioAdded);

            } else if (payload instanceof ScenarioRemovedEventAvro) {
                ScenarioRemovedEventAvro scenarioRemoved = (ScenarioRemovedEventAvro) payload;
                scenarioService.removeScenario(event.getHubId(), scenarioRemoved.getName());
            }
        } catch (Exception e) {
            log.error("Error processing hub event: {}", event, e);
        }
    }

    private void convertAndSaveScenario(String hubId, ScenarioAddedEventAvro scenarioAdded) {
        Scenario scenario = Scenario.builder()
                .hubId(hubId)
                .name(scenarioAdded.getName())
                .build();

        Map<String, Condition> sensorConditions = new HashMap<>();
        Map<String, Action> sensorActions = new HashMap<>();

        for (ScenarioConditionAvro conditionAvro : scenarioAdded.getConditions()) {
            Condition condition = Condition.builder()
                    .type(ConditionType.valueOf(conditionAvro.getType().name()))
                    .operation(OperationType.valueOf(conditionAvro.getOperation().name()))
                    .value((Integer) conditionAvro.getValue())
                    .build();

            sensorConditions.put(conditionAvro.getSensorId(), condition);
        }

        for (DeviceActionAvro actionAvro : scenarioAdded.getActions()) {
            Action action = Action.builder()
                    .type(ConditionType.valueOf(actionAvro.getType().name()))
                    .value(actionAvro.getValue() != null ? actionAvro.getValue() : 0)
                    .build();

            sensorActions.put(actionAvro.getSensorId(), action);
        }

        scenarioService.addScenario(scenario, sensorConditions, sensorActions);
    }

    public void shutdown() {
        running = false;
    }
}
package ru.yandex.practicum.telemetry.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.telemetry.analyzer.model.*;
import ru.yandex.practicum.telemetry.analyzer.repository.*;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScenarioService {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Transactional
    public void addSensor(Sensor sensor) {
        sensorRepository.save(sensor);
        log.info("Added sensor: id={}, hubId={}", sensor.getId(), sensor.getHubId());
    }

    @Transactional
    public void removeSensor(String hubId, String sensorId) {
        sensorRepository.deleteByHubIdAndId(hubId, sensorId);
        log.info("Removed sensor: id={}, hubId={}", sensorId, hubId);
    }

    @Transactional
    public void addScenario(Scenario scenario, Map<String, Condition> sensorConditions,
                            Map<String, Action> sensorActions) {
        Scenario savedScenario = scenarioRepository.save(scenario);

        sensorConditions.forEach((sensorId, condition) -> {
            Sensor sensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new RuntimeException("Sensor not found: " + sensorId));
            Condition savedCondition = conditionRepository.save(condition);

            ScenarioCondition scenarioCondition = ScenarioCondition.builder()
                    .id(new ScenarioConditionId(savedScenario.getId(), sensorId, savedCondition.getId()))
                    .scenario(savedScenario)
                    .sensor(sensor)
                    .condition(savedCondition)
                    .build();

            savedScenario.getScenarioConditions().add(scenarioCondition);
        });

        sensorActions.forEach((sensorId, action) -> {
            Sensor sensor = sensorRepository.findById(sensorId)
                    .orElseThrow(() -> new RuntimeException("Sensor not found: " + sensorId));
            Action savedAction = actionRepository.save(action);

            ScenarioAction scenarioAction = ScenarioAction.builder()
                    .id(new ScenarioActionId(savedScenario.getId(), sensorId, savedAction.getId()))
                    .scenario(savedScenario)
                    .sensor(sensor)
                    .action(savedAction)
                    .build();

            savedScenario.getScenarioActions().add(scenarioAction);
        });

        scenarioRepository.save(savedScenario);
        log.info("Added scenario: name={}, hubId={}", scenario.getName(), scenario.getHubId());
    }

    @Transactional
    public void removeScenario(String hubId, String name) {
        scenarioRepository.deleteByHubIdAndName(hubId, name);
        log.info("Removed scenario: name={}, hubId={}", name, hubId);
    }

    public Map<String, Action> analyzeSnapshot(SensorsSnapshotAvro snapshot) {
        String hubId = snapshot.getHubId();
        Map<String, SensorStateAvro> sensorStates = snapshot.getSensorsState();

        List<Scenario> scenarios = scenarioRepository.findByHubId(hubId);
        log.debug("Found {} scenarios for hub {}", scenarios.size(), hubId);

        Map<String, Action> actionsToExecute = new HashMap<>();

        for (Scenario scenario : scenarios) {
            boolean allConditionsMet = scenario.getScenarioConditions().stream()
                    .allMatch(sc -> {
                        String sensorId = sc.getSensor().getId();
                        SensorStateAvro state = sensorStates.get(sensorId);
                        return state != null && state.getData() != null &&
                                evaluateCondition(sc.getCondition(), state);
                    });

            if (allConditionsMet && !scenario.getScenarioConditions().isEmpty()) {
                scenario.getScenarioActions().forEach(sa -> {
                    actionsToExecute.put(sa.getSensor().getId(), sa.getAction());
                });
                log.info("Scenario '{}' triggered for hub {}", scenario.getName(), hubId);
            }
        }

        return actionsToExecute;
    }

    private boolean evaluateCondition(Condition condition, SensorStateAvro state) {
        if (!matchesConditionType(state, condition.getType())) {
            return false;
        }

        int sensorValue = extractSensorValue(state);
        return evaluateOperation(sensorValue, condition.getOperation(), condition.getValue());
    }

    private boolean matchesConditionType(SensorStateAvro state, ConditionType type) {
        switch (type) {
            case TEMPERATURE:
                return state.getData() instanceof TemperatureSensorAvro;
            case HUMIDITY:
                return state.getData() instanceof ClimateSensorAvro;
            case LUMINOSITY:
                return state.getData() instanceof LightSensorAvro;
            case CO2LEVEL:
                return state.getData() instanceof ClimateSensorAvro;
            case MOTION:
                return state.getData() instanceof MotionSensorAvro;
            case SWITCH:
                return state.getData() instanceof SwitchSensorAvro;
            default:
                return false;
        }
    }

    private boolean evaluateOperation(int sensorValue, OperationType operation, Integer targetValue) {
        switch (operation) {
            case EQUALS:
                return sensorValue == targetValue;
            case GREATER_THAN:
                return sensorValue > targetValue;
            case LOWER_THAN:
                return sensorValue < targetValue;
            default:
                return false;
        }
    }

    private int extractSensorValue(SensorStateAvro state) {
        if (state.getData() instanceof TemperatureSensorAvro) {
            return ((TemperatureSensorAvro) state.getData()).getTemperatureC();
        } else if (state.getData() instanceof ClimateSensorAvro) {
            ClimateSensorAvro climate = (ClimateSensorAvro) state.getData();
            // Возвращаем humidity или temperature_c или co2_level в зависимости от контекста
            return climate.getHumidity();
        } else if (state.getData() instanceof LightSensorAvro) {
            return ((LightSensorAvro) state.getData()).getLuminosity();
        } else if (state.getData() instanceof MotionSensorAvro) {
            return ((MotionSensorAvro) state.getData()).getMotion() ? 1 : 0;
        } else if (state.getData() instanceof SwitchSensorAvro) {
            return ((SwitchSensorAvro) state.getData()).getState() ? 1 : 0;
        }
        return 0;
    }
}
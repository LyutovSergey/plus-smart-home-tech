package ru.yandex.practicum.telemetry.collector.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.hub.*;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class HubEventMapper {

    /**
     * Главный метод: преобразует JSON-событие хаба в Avro-событие.
     */
    public HubEventAvro toAvro(HubEvent event) {
        if (event == null) {
            return null;
        }

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }

    /**
     * Маппинг конкретного payload в зависимости от типа события хаба.
     */
    private Object mapPayload(HubEvent event) {
        return switch (event.getType()) {
            case DEVICE_ADDED -> toDeviceAddedEventAvro((DeviceAddedEvent) event);
            case DEVICE_REMOVED -> toDeviceRemovedEventAvro((DeviceRemovedEvent) event);
            case SCENARIO_ADDED -> toScenarioAddedEventAvro((ScenarioAddedEvent) event);
            case SCENARIO_REMOVED -> toScenarioRemovedEventAvro((ScenarioRemovedEvent) event);
        };
    }

    // ===== ОТДЕЛЬНЫЕ МАППЕРЫ ДЛЯ КАЖДОГО ТИПА СОБЫТИЙ =====

    /**
     * DeviceAddedEvent → DeviceAddedEventAvro
     */
    private DeviceAddedEventAvro toDeviceAddedEventAvro(DeviceAddedEvent event) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(event.getId())
                .setType(mapDeviceType(event.getDeviceType()))
                .build();
    }

    /**
     * DeviceRemovedEvent → DeviceRemovedEventAvro
     */
    private DeviceRemovedEventAvro toDeviceRemovedEventAvro(DeviceRemovedEvent event) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(event.getId())
                .build();
    }

    /**
     * ScenarioAddedEvent → ScenarioAddedEventAvro
     */
    private ScenarioAddedEventAvro toScenarioAddedEventAvro(ScenarioAddedEvent event) {
        return ScenarioAddedEventAvro.newBuilder()
                .setName(event.getName())
                .setConditions(mapConditions(event.getConditions()))
                .setActions(mapActions(event.getActions()))
                .build();
    }

    /**
     * ScenarioRemovedEvent → ScenarioRemovedEventAvro
     */
    private ScenarioRemovedEventAvro toScenarioRemovedEventAvro(ScenarioRemovedEvent event) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(event.getName())
                .build();
    }

    // ===== МАППЕРЫ ДЛЯ ВЛОЖЕННЫХ ОБЪЕКТОВ =====

    /**
     * Маппинг списка условий.
     */
    private List<ScenarioConditionAvro> mapConditions(List<ScenarioCondition> conditions) {
        return conditions.stream()
                .map(this::toScenarioConditionAvro)
                .collect(Collectors.toList());
    }

    /**
     * ScenarioCondition → ScenarioConditionAvro
     */
    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioCondition condition) {
        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(mapConditionType(condition.getType()))
                .setOperation(mapConditionOperation(condition.getOperation()))
                .setValue(condition.getValue() != null ? condition.getValue() : null)
                .build();
    }

    /**
     * Маппинг списка действий.
     */
    private List<DeviceActionAvro> mapActions(List<DeviceAction> actions) {
        return actions.stream()
                .map(this::toDeviceActionAvro)
                .collect(Collectors.toList());
    }

    /**
     * DeviceAction → DeviceActionAvro
     */
    private DeviceActionAvro toDeviceActionAvro(DeviceAction action) {
        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(mapActionType(action.getType()))
                .setValue(action.getValue() != null ? action.getValue() : null)
                .build();
    }

    // ===== МАППЕРЫ ДЛЯ ПЕРЕЧИСЛЕНИЙ (ENUM) =====

    /**
     * DeviceType → DeviceTypeAvro
     */
    private DeviceTypeAvro mapDeviceType(DeviceType deviceType) {
        if (deviceType == null) {
            return null;
        }
        return switch (deviceType) {
            case MOTION_SENSOR -> DeviceTypeAvro.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceTypeAvro.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceTypeAvro.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceTypeAvro.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceTypeAvro.SWITCH_SENSOR;
        };
    }

    /**
     * ConditionType → ConditionTypeAvro
     */
    private ConditionTypeAvro mapConditionType(ConditionType conditionType) {
        if (conditionType == null) {
            return null;
        }
        return switch (conditionType) {
            case MOTION -> ConditionTypeAvro.MOTION;
            case LUMINOSITY -> ConditionTypeAvro.LUMINOSITY;
            case SWITCH -> ConditionTypeAvro.SWITCH;
            case TEMPERATURE -> ConditionTypeAvro.TEMPERATURE;
            case CO2LEVEL -> ConditionTypeAvro.CO2LEVEL;
            case HUMIDITY -> ConditionTypeAvro.HUMIDITY;
        };
    }

    /**
     * ConditionOperation → ConditionOperationAvro
     */
    private ConditionOperationAvro mapConditionOperation(ConditionOperation operation) {
        if (operation == null) {
            return null;
        }
        return switch (operation) {
            case EQUALS -> ConditionOperationAvro.EQUALS;
            case GREATER_THAN -> ConditionOperationAvro.GREATER_THAN;
            case LOWER_THAN -> ConditionOperationAvro.LOWER_THAN;
        };
    }

    /**
     * ActionType → ActionTypeAvro
     */
    private ActionTypeAvro mapActionType(ActionType actionType) {
        if (actionType == null) {
            return null;
        }
        return switch (actionType) {
            case ACTIVATE -> ActionTypeAvro.ACTIVATE;
            case DEACTIVATE -> ActionTypeAvro.DEACTIVATE;
            case INVERSE -> ActionTypeAvro.INVERSE;
            case SET_VALUE -> ActionTypeAvro.SET_VALUE;
        };
    }
}
package ru.yandex.practicum.telemetry.collector.mapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.telemetry.collector.model.hub.*;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class HubEventMapper {

    // ===== Protobuf → бизнес-модель =====

    public HubEvent toHubEvent(HubEventProto proto) {
        if (proto == null) {
            return null;
        }

        HubEvent event = switch (proto.getPayloadCase()) {
            case DEVICE_ADDED -> {
                var p = proto.getDeviceAdded();
                DeviceAddedEvent e = new DeviceAddedEvent();
                e.setId(p.getId());
                e.setDeviceType(toDeviceType(p.getType()));
                yield e;
            }
            case DEVICE_REMOVED -> {
                var p = proto.getDeviceRemoved();
                DeviceRemovedEvent e = new DeviceRemovedEvent();
                e.setId(p.getId());
                yield e;
            }
            case SCENARIO_ADDED -> {
                var p = proto.getScenarioAdded();
                ScenarioAddedEvent e = new ScenarioAddedEvent();
                e.setName(p.getName());
                e.setConditions(p.getConditionList().stream()
                        .map(this::toScenarioCondition)
                        .collect(Collectors.toList()));
                e.setActions(p.getActionList().stream()
                        .map(this::toDeviceAction)
                        .collect(Collectors.toList()));
                yield e;
            }
            case SCENARIO_REMOVED -> {
                var p = proto.getScenarioRemoved();
                ScenarioRemovedEvent e = new ScenarioRemovedEvent();
                e.setName(p.getName());
                yield e;
            }
            case PAYLOAD_NOT_SET -> throw new IllegalArgumentException("Payload not set in HubEventProto");
        };

        event.setHubId(proto.getHubId());
        long millis = proto.getTimestamp().getSeconds() * 1000 + proto.getTimestamp().getNanos() / 1000000;
        event.setTimestamp(Instant.ofEpochMilli(millis));

        return event;
    }

    // ===== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ Protobuf → бизнес-модель =====

    private ScenarioCondition toScenarioCondition(
            ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto proto) {
        ScenarioCondition condition = new ScenarioCondition();
        condition.setSensorId(proto.getSensorId());
        condition.setType(toConditionType(proto.getType()));
        condition.setOperation(toConditionOperation(proto.getOperation()));

        if (proto.hasBoolValue()) {
            condition.setValue(proto.getBoolValue());
        } else if (proto.hasIntValue()) {
            condition.setValue(proto.getIntValue());
        }

        return condition;
    }

    private DeviceAction toDeviceAction(
            ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto proto) {
        DeviceAction action = new DeviceAction();
        action.setSensorId(proto.getSensorId());
        action.setType(toActionType(proto.getType()));
        if (proto.hasValue()) {
            action.setValue(proto.getValue());
        }
        return action;
    }

    private DeviceType toDeviceType(
            ru.yandex.practicum.grpc.telemetry.event.DeviceTypeProto proto) {
        return switch (proto) {
            case MOTION_SENSOR -> DeviceType.MOTION_SENSOR;
            case TEMPERATURE_SENSOR -> DeviceType.TEMPERATURE_SENSOR;
            case LIGHT_SENSOR -> DeviceType.LIGHT_SENSOR;
            case CLIMATE_SENSOR -> DeviceType.CLIMATE_SENSOR;
            case SWITCH_SENSOR -> DeviceType.SWITCH_SENSOR;
            default -> throw new IllegalArgumentException("Unknown device type: " + proto);
        };
    }

    private ConditionType toConditionType(
            ru.yandex.practicum.grpc.telemetry.event.ConditionTypeProto proto) {
        return switch (proto) {
            case MOTION -> ConditionType.MOTION;
            case LUMINOSITY -> ConditionType.LUMINOSITY;
            case SWITCH -> ConditionType.SWITCH;
            case TEMPERATURE -> ConditionType.TEMPERATURE;
            case CO2LEVEL -> ConditionType.CO2LEVEL;
            case HUMIDITY -> ConditionType.HUMIDITY;
            default -> throw new IllegalArgumentException("Unknown condition type: " + proto);
        };
    }

    private ConditionOperation toConditionOperation(
            ru.yandex.practicum.grpc.telemetry.event.ConditionOperationProto proto) {
        return switch (proto) {
            case EQUALS -> ConditionOperation.EQUALS;
            case GREATER_THAN -> ConditionOperation.GREATER_THAN;
            case LOWER_THAN -> ConditionOperation.LOWER_THAN;
            default -> throw new IllegalArgumentException("Unknown condition operation: " + proto);
        };
    }

    private ActionType toActionType(
            ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto proto) {
        return switch (proto) {
            case ACTIVATE -> ActionType.ACTIVATE;
            case DEACTIVATE -> ActionType.DEACTIVATE;
            case INVERSE -> ActionType.INVERSE;
            case SET_VALUE -> ActionType.SET_VALUE;
            default -> throw new IllegalArgumentException("Unknown action type: " + proto);
        };
    }

    // ===== бизнес-модель → Avro =====

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

    private Object mapPayload(HubEvent event) {
        if (event instanceof DeviceAddedEvent) {
            return toDeviceAddedEventAvro((DeviceAddedEvent) event);
        } else if (event instanceof DeviceRemovedEvent) {
            return toDeviceRemovedEventAvro((DeviceRemovedEvent) event);
        } else if (event instanceof ScenarioAddedEvent) {
            return toScenarioAddedEventAvro((ScenarioAddedEvent) event);
        } else if (event instanceof ScenarioRemovedEvent) {
            return toScenarioRemovedEventAvro((ScenarioRemovedEvent) event);
        }
        throw new IllegalArgumentException("Unknown event type: " + event.getClass().getSimpleName());
    }

    private DeviceAddedEventAvro toDeviceAddedEventAvro(DeviceAddedEvent event) {
        return DeviceAddedEventAvro.newBuilder()
                .setId(event.getId())
                .setType(mapDeviceType(event.getDeviceType()))
                .build();
    }

    private DeviceRemovedEventAvro toDeviceRemovedEventAvro(DeviceRemovedEvent event) {
        return DeviceRemovedEventAvro.newBuilder()
                .setId(event.getId())
                .build();
    }

    private ScenarioAddedEventAvro toScenarioAddedEventAvro(ScenarioAddedEvent event) {
        return ScenarioAddedEventAvro.newBuilder()
                .setName(event.getName())
                .setConditions(mapConditions(event.getConditions()))
                .setActions(mapActions(event.getActions()))
                .build();
    }

    private ScenarioRemovedEventAvro toScenarioRemovedEventAvro(ScenarioRemovedEvent event) {
        return ScenarioRemovedEventAvro.newBuilder()
                .setName(event.getName())
                .build();
    }

    private List<ScenarioConditionAvro> mapConditions(List<ScenarioCondition> conditions) {
        if (conditions == null) {
            return null;
        }
        return conditions.stream()
                .map(this::toScenarioConditionAvro)
                .collect(Collectors.toList());
    }

    private ScenarioConditionAvro toScenarioConditionAvro(ScenarioCondition condition) {
        // ИСПРАВЛЕНО: используем просто setValue()
        ScenarioConditionAvro.Builder builder = ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(mapConditionType(condition.getType()))
                .setOperation(mapConditionOperation(condition.getOperation()));

        // В Avro поле называется просто "value"
        Object value = condition.getValue();
        if (value != null) {
            // Если value - это Boolean или Integer, просто устанавливаем
            builder.setValue(value);
        }

        return builder.build();
    }

    private List<DeviceActionAvro> mapActions(List<DeviceAction> actions) {
        if (actions == null) {
            return null;
        }
        return actions.stream()
                .map(this::toDeviceActionAvro)
                .collect(Collectors.toList());
    }

    private DeviceActionAvro toDeviceActionAvro(DeviceAction action) {
        DeviceActionAvro.Builder builder = DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(mapActionType(action.getType()));

        if (action.getValue() != null) {
            builder.setValue(action.getValue());
        }

        return builder.build();
    }

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
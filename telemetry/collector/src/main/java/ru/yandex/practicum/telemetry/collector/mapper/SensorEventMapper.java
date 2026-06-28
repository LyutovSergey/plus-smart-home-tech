package ru.yandex.practicum.telemetry.collector.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.*;
import ru.yandex.practicum.grpc.telemetry.event.*;
import ru.yandex.practicum.telemetry.collector.model.sensor.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
public class SensorEventMapper {

    // ===== Protobuf → бизнес-модель =====

    public ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent toSensorEvent(
            ru.yandex.practicum.grpc.telemetry.event.SensorEventProto proto) {
        if (proto == null) {
            return null;
        }

        ru.yandex.practicum.grpc.telemetry.event.SensorEventProto.PayloadCase payloadCase =
                proto.getPayloadCase();

        ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent event = switch (payloadCase) {
            case MOTION_SENSOR -> {
                MotionSensorProto motion = proto.getMotionSensor();
                ru.yandex.practicum.telemetry.collector.model.sensor.MotionSensorEvent e =
                        new ru.yandex.practicum.telemetry.collector.model.sensor.MotionSensorEvent();
                e.setLinkQuality(motion.getLinkQuality());
                e.setMotion(motion.getMotion());
                e.setVoltage(motion.getVoltage());
                yield e;
            }
            case TEMPERATURE_SENSOR -> {
                TemperatureSensorProto temp = proto.getTemperatureSensor();
                ru.yandex.practicum.telemetry.collector.model.sensor.TemperatureSensorEvent e =
                        new ru.yandex.practicum.telemetry.collector.model.sensor.TemperatureSensorEvent();
                e.setTemperatureC(temp.getTemperatureC());
                e.setTemperatureF(temp.getTemperatureF());
                yield e;
            }
            case LIGHT_SENSOR -> {
                LightSensorProto light = proto.getLightSensor();
                ru.yandex.practicum.telemetry.collector.model.sensor.LightSensorEvent e =
                        new ru.yandex.practicum.telemetry.collector.model.sensor.LightSensorEvent();
                e.setLinkQuality(light.getLinkQuality());
                e.setLuminosity(light.getLuminosity());
                yield e;
            }
            case CLIMATE_SENSOR -> {
                ClimateSensorProto climate = proto.getClimateSensor();
                ru.yandex.practicum.telemetry.collector.model.sensor.ClimateSensorEvent e =
                        new ru.yandex.practicum.telemetry.collector.model.sensor.ClimateSensorEvent();
                e.setTemperatureC(climate.getTemperatureC());
                e.setHumidity(climate.getHumidity());
                e.setCo2Level(climate.getCo2Level());
                yield e;
            }
            case SWITCH_SENSOR -> {
                SwitchSensorProto switchSensor = proto.getSwitchSensor();
                ru.yandex.practicum.telemetry.collector.model.sensor.SwitchSensorEvent e =
                        new ru.yandex.practicum.telemetry.collector.model.sensor.SwitchSensorEvent();
                e.setState(switchSensor.getState());
                yield e;
            }
            default -> throw new IllegalArgumentException("Unknown sensor type: " + payloadCase);
        };

        event.setId(proto.getId());
        event.setHubId(proto.getHubId());
        // ИСПРАВЛЕНО: используем Instant вместо LocalDateTime
        event.setTimestamp(toInstant(proto.getTimestamp()));

        return event;
    }

    private Instant toInstant(com.google.protobuf.Timestamp timestamp) {
        if (timestamp == null) {
            return Instant.now();
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }

    // ===== бизнес-модель → Avro =====

    public SensorEventAvro toAvro(ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent event) {
        if (event == null) {
            return null;
        }

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(event.getTimestamp())
                .setPayload(mapPayload(event))
                .build();
    }

    private Object mapPayload(ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent event) {
        // Определяем тип через instanceof
        if (event instanceof ru.yandex.practicum.telemetry.collector.model.sensor.ClimateSensorEvent) {
            return toClimateSensorAvro(
                    (ru.yandex.practicum.telemetry.collector.model.sensor.ClimateSensorEvent) event);
        } else if (event instanceof ru.yandex.practicum.telemetry.collector.model.sensor.LightSensorEvent) {
            return toLightSensorAvro(
                    (ru.yandex.practicum.telemetry.collector.model.sensor.LightSensorEvent) event);
        } else if (event instanceof ru.yandex.practicum.telemetry.collector.model.sensor.MotionSensorEvent) {
            return toMotionSensorAvro(
                    (ru.yandex.practicum.telemetry.collector.model.sensor.MotionSensorEvent) event);
        } else if (event instanceof ru.yandex.practicum.telemetry.collector.model.sensor.SwitchSensorEvent) {
            return toSwitchSensorAvro(
                    (ru.yandex.practicum.telemetry.collector.model.sensor.SwitchSensorEvent) event);
        } else if (event instanceof ru.yandex.practicum.telemetry.collector.model.sensor.TemperatureSensorEvent) {
            return toTemperatureSensorAvro(
                    (ru.yandex.practicum.telemetry.collector.model.sensor.TemperatureSensorEvent) event);
        }
        throw new IllegalArgumentException("Unknown sensor event type: " + event.getClass().getSimpleName());
    }

    private ClimateSensorAvro toClimateSensorAvro(
            ru.yandex.practicum.telemetry.collector.model.sensor.ClimateSensorEvent event) {
        return ClimateSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperatureC())
                .setHumidity(event.getHumidity())
                .setCo2Level(event.getCo2Level())
                .build();
    }

    private LightSensorAvro toLightSensorAvro(
            ru.yandex.practicum.telemetry.collector.model.sensor.LightSensorEvent event) {
        return LightSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality())
                .setLuminosity(event.getLuminosity())
                .build();
    }

    private MotionSensorAvro toMotionSensorAvro(
            ru.yandex.practicum.telemetry.collector.model.sensor.MotionSensorEvent event) {
        return MotionSensorAvro.newBuilder()
                .setLinkQuality(event.getLinkQuality())
                .setMotion(event.getMotion())
                .setVoltage(event.getVoltage())
                .build();
    }

    private SwitchSensorAvro toSwitchSensorAvro(
            ru.yandex.practicum.telemetry.collector.model.sensor.SwitchSensorEvent event) {
        return SwitchSensorAvro.newBuilder()
                .setState(event.getState())
                .build();
    }

    private TemperatureSensorAvro toTemperatureSensorAvro(
            ru.yandex.practicum.telemetry.collector.model.sensor.TemperatureSensorEvent event) {
        return TemperatureSensorAvro.newBuilder()
                .setTemperatureC(event.getTemperatureC())
                .setTemperatureF(event.getTemperatureF())
                .build();
    }
}
package ru.yandex.practicum.telemetry.collector.model.sensor;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class ClimateSensorEvent extends SensorEvent {

    @PositiveOrZero
    private Integer temperatureC;

    @PositiveOrZero
    private Integer humidity;

    @PositiveOrZero
    private Integer co2Level;

    @Override
    public SensorEventType getType() {
        return SensorEventType.CLIMATE_SENSOR_EVENT;
    }
}
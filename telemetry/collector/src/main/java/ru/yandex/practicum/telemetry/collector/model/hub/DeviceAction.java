package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class DeviceAction {

    @NotBlank(message = "sensorId must not be blank")
    private String sensorId;

    @NotNull(message = "type must not be null")
    private ActionType type;

    private Integer value;
}
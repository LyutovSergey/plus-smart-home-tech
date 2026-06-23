package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ScenarioCondition {

    @NotBlank(message = "sensorId must not be blank")
    private String sensorId;

    @NotNull(message = "type must not be null")
    private ConditionType type;

    @NotNull(message = "operation must not be null")
    private ConditionOperation operation;

    private Object value;  // ← ИЗМЕНЕНО: Integer → Object
}
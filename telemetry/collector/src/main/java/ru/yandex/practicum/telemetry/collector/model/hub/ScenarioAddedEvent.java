package ru.yandex.practicum.telemetry.collector.model.hub;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString(callSuper = true)
public class ScenarioAddedEvent extends HubEvent {

    @NotBlank(message = "name must not be blank")
    @Size(min = 3, message = "name must be at least 3 characters")
    private String name;

    @NotEmpty(message = "conditions must not be empty")
    @Valid
    private List<ScenarioCondition> conditions;

    @NotEmpty(message = "actions must not be empty")
    @Valid
    private List<DeviceAction> actions;

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_ADDED;
    }
}
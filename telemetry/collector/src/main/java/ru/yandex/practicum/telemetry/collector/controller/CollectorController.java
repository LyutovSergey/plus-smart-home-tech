package ru.yandex.practicum.telemetry.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.service.hub.HubEventProcessor;
import ru.yandex.practicum.telemetry.collector.service.sensor.SensorEventProcessor;

@Slf4j
@RestController
@RequestMapping("/events")  // ← ДОБАВИТЬ ЭТУ СТРОКУ!
@RequiredArgsConstructor
public class CollectorController {

    private final SensorEventProcessor sensorEventProcessor;
    private final HubEventProcessor hubEventProcessor;

    /**
     * Эндпоинт для приёма показаний датчиков
     * POST /events/sensors
     */
    @PostMapping("/sensors")
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        log.info("Received sensor event: type={}, id={}, hubId={}",
                event.getType(), event.getId(), event.getHubId());
        sensorEventProcessor.processEvent(event);
    }

    /**
     * Эндпоинт для приёма событий хаба
     * POST /events/hubs
     */
    @PostMapping("/hubs")
    public void collectHubEvent(@Valid @RequestBody HubEvent event) {
        log.info("Received hub event: type={}, hubId={}",
                event.getType(), event.getHubId());
        hubEventProcessor.processEvent(event);
    }
}
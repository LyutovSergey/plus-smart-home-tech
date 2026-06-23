package ru.yandex.practicum.telemetry.collector.service.hub;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.telemetry.collector.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.service.kafka.KafkaEventProducer;

@Slf4j
@Service
@RequiredArgsConstructor
public class HubEventProcessor {

    private final HubEventMapper mapper;
    private final KafkaEventProducer producer;

    public void processEvent(HubEvent event) {
        log.debug("Processing hub event: {}", event);

        // Преобразуем JSON-событие в Avro-объект
        var avroEvent = mapper.toAvro(event);

        // Отправляем в Kafka топик telemetry.hubs.v1
        producer.sendHubEvent(avroEvent);
    }
}
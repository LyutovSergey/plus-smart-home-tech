package ru.yandex.practicum.telemetry.collector.service.sensor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.mapper.SensorEventMapper;
import ru.yandex.practicum.telemetry.collector.service.kafka.KafkaEventProducer;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorEventProcessor {

    private final SensorEventMapper mapper;
    private final KafkaEventProducer producer;

    public void processEvent(SensorEvent event) {
        log.debug("Processing sensor event: {}", event);

        // Преобразуем JSON-событие в Avro-объект
        var avroEvent = mapper.toAvro(event);

        // Отправляем в Kafka топик telemetry.sensors.v1
        producer.sendSensorEvent(avroEvent);
    }
}
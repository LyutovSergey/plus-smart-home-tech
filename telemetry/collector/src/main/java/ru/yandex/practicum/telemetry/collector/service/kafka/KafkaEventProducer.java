package ru.yandex.practicum.telemetry.collector.service.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topic.sensor-events}")
    private String sensorEventsTopic;

    @Value("${kafka.topic.hub-events}")
    private String hubEventsTopic;

    public void sendSensorEvent(SensorEventAvro event) {
        sendEvent(sensorEventsTopic, event.getId(), event);
    }

    public void sendHubEvent(HubEventAvro event) {
        sendEvent(hubEventsTopic, event.getHubId(), event);
    }

    private void sendEvent(String topic, String key, Object event) {
        log.debug("Sending event to topic {}: {}", topic, event);
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.debug("Successfully sent event to topic {}: offset={}",
                                topic, result.getRecordMetadata().offset());
                    } else {
                        log.error("Failed to send event to topic {}", topic, ex);
                    }
                });
    }
}
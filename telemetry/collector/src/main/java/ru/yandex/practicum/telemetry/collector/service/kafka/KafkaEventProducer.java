package ru.yandex.practicum.telemetry.collector.service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Slf4j
@Service
public class KafkaEventProducer {

    @Value("${kafka.topic.sensor-events}")
    private String sensorEventsTopic;

    @Value("${kafka.topic.hub-events}")
    private String hubEventsTopic;

    private final Producer<String, SpecificRecordBase> kafkaProducer;

    public KafkaEventProducer(Producer<String, SpecificRecordBase> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    public void sendSensorEvent(SensorEventAvro event) {
        sendEvent(sensorEventsTopic, event.getId(), event);
    }

    public void sendHubEvent(HubEventAvro event) {
        sendEvent(hubEventsTopic, event.getHubId(), event);
    }

    private void sendEvent(String topic, String key, SpecificRecordBase  event) {
        log.debug("Sending event to topic {}: {}", topic, event);
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(topic, key, event);
        kafkaProducer.send(record, (RecordMetadata metadata, Exception ex) -> {
            if (ex == null) {
                log.debug("Successfully sent event to topic {}: offset={}",
                        topic, metadata.offset());
            } else {
                // Если GeneralAvroSerializer выбросит SerializationException, она также придет сюда в 'ex'
                log.error("Failed to send event to topic {}", topic, ex);
            }
        });
    }
}
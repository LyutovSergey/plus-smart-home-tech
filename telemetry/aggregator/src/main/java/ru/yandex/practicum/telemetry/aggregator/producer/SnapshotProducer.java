package ru.yandex.practicum.telemetry.aggregator.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.config.KafkaConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProducer {

    private final KafkaProducer<String, SensorsSnapshotAvro> producer;
    private final KafkaConfig kafkaConfig;

    public void send(SensorsSnapshotAvro snapshot) {
        if (snapshot == null) {
            return;
        }

        String key = snapshot.getHubId();
        String topic = kafkaConfig.getSnapshotsTopic();

        ProducerRecord<String, SensorsSnapshotAvro> record = new ProducerRecord<>(topic, key, snapshot);

        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                log.error("Ошибка отправки снапшота для хаба {}: {}", key, exception.getMessage());
            } else {
                log.debug("Снапшот для хаба {} отправлен в топик {}", key, metadata.topic());
            }
        });
    }

    public void flush() {
        try {
            producer.flush();
        } catch (Exception e) {
            log.error("Ошибка при сбросе продюсера: {}", e.getMessage());
        }
    }

    public void close() {
        try {
            producer.close();
        } catch (Exception e) {
            log.error("Ошибка при закрытии продюсера: {}", e.getMessage());
        }
    }
}
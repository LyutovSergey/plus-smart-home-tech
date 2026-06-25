package ru.yandex.practicum.telemetry.aggregator.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.config.KafkaConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnapshotProducer {

    private final KafkaProducer<String, byte[]> producer;
    private final KafkaConfig kafkaConfig;

    public void send(SensorsSnapshotAvro snapshot) {
        log.info("send() вызван для хаба {}", snapshot != null ? snapshot.getHubId() : "null");

        if (snapshot == null) {
            log.warn("Попытка отправить null снапшот");
            return;
        }

        String key = snapshot.getHubId();
        String topic = kafkaConfig.getSnapshotsTopic();

        log.info("Отправка в топик: {}, ключ: {}", topic, key);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<SensorsSnapshotAvro> writer = new SpecificDatumWriter<>(SensorsSnapshotAvro.class);
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(snapshot, encoder);
            encoder.flush();

            byte[] serializedValue = outputStream.toByteArray();
            log.info("Сериализовано {} байт", serializedValue.length);

            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, serializedValue);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Ошибка отправки снапшота для хаба {}: {}", key, exception.getMessage());
                } else {
                    log.info("Снапшот для хаба {} отправлен в топик {}, оффсет {}", key, metadata.topic(), metadata.offset());
                }
            });

        } catch (IOException e) {
            log.error("Ошибка сериализации снапшота для хаба {}: {}", key, e.getMessage());
        }
    }

    public void flush() {
        try {
            producer.flush();
            log.debug("Продюсер сброшен");
        } catch (Exception e) {
            log.error("Ошибка при сбросе продюсера: {}", e.getMessage());
        }
    }

    public void close() {
        try {
            producer.close();
            log.debug("Продюсер закрыт");
        } catch (Exception e) {
            log.error("Ошибка при закрытии продюсера: {}", e.getMessage());
        }
    }
}
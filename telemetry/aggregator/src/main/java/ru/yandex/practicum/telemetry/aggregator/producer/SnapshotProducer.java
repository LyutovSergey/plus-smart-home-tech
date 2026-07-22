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

        log.info("--- ДАННЫЕ СНАПШОТА ---");
        log.info("Хаб: {}", key);
        log.info("Топик: {}", topic);
        log.info("Временная метка снапшота: {}", snapshot.getTimestamp());
        log.info("Количество датчиков в снапшоте: {}", snapshot.getSensorsState().size());

        snapshot.getSensorsState().forEach((sensorId, state) -> {
            log.info("  Датчик: {}", sensorId);
            log.info("    Время: {}", state.getTimestamp());
            log.info("    Данные: {}", state.getData());
        });
        log.info("--- КОНЕЦ ДАННЫХ СНАПШОТА ---");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<SensorsSnapshotAvro> writer = new SpecificDatumWriter<>(SensorsSnapshotAvro.class);
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);
            writer.write(snapshot, encoder);
            encoder.flush();

            byte[] serializedValue = outputStream.toByteArray();

            log.info("--- ОТПРАВКА В KAFKA ---");
            log.info("Топик: {}", topic);
            log.info("Ключ: {}", key);
            log.info("Размер данных: {} байт", serializedValue.length);
            log.info("--- КОНЕЦ ОТПРАВКИ ---");

            ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, serializedValue);

            producer.send(record, (metadata, exception) -> {
                if (exception != null) {
                    log.error("Ошибка отправки снапшота для хаба {}: {}", key, exception.getMessage());
                } else {
                    log.info("Снапшот для хаба {} успешно отправлен:", key);
                    log.info("   Топик: {}", metadata.topic());
                    log.info("   Партиция: {}", metadata.partition());
                    log.info("   Оффсет: {}", metadata.offset());
                    log.info("   Время: {}", metadata.timestamp());
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
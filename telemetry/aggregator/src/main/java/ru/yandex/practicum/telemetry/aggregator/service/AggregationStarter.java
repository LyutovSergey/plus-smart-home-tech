package ru.yandex.practicum.telemetry.aggregator.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.config.KafkaConfig;
import ru.yandex.practicum.telemetry.aggregator.producer.SnapshotProducer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final SnapshotProducer snapshotProducer;
    private final SnapshotManager snapshotManager;
    private final KafkaConfig kafkaConfig;

    private volatile boolean running = true;

    @PostConstruct
    public void start() {
        log.info("Запуск AggregationStarter...");

        try {
            String sensorsTopic = kafkaConfig.getSensorsTopic();
            consumer.subscribe(List.of(sensorsTopic));
            log.info("Подписка на топик {} выполнена", sensorsTopic);

            while (running) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(Duration.ofMillis(100));

                if (records.isEmpty()) {
                    continue;
                }

                log.debug("Получено {} записей", records.count());

                records.forEach(record -> {
                    SensorEventAvro event = record.value();

                    if (event == null) {
                        log.warn("Получено пустое событие, пропускаем");
                        return;
                    }

                    log.debug("Обработка события от датчика {} (хаб {})", event.getId(), event.getHubId());

                    Optional<SensorsSnapshotAvro> updatedSnapshot = snapshotManager.updateState(event);
                    updatedSnapshot.ifPresent(snapshotProducer::send);
                });

                try {
                    consumer.commitSync();
                    log.debug("Смещения зафиксированы");
                } catch (Exception e) {
                    log.error("Ошибка при фиксации смещений: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Ошибка в цикле обработки событий", e);
        } finally {
            shutdown();
        }
    }

    public void stop() {
        running = false;
    }

    private void shutdown() {
        log.info("Завершение работы AggregationStarter...");

        try {
            consumer.commitSync();
        } catch (Exception e) {
            log.warn("Ошибка при фиксации смещений: {}", e.getMessage());
        }

        try {
            snapshotProducer.flush();
            snapshotProducer.close();
        } catch (Exception e) {
            log.warn("Ошибка при закрытии продюсера: {}", e.getMessage());
        }

        try {
            consumer.close();
        } catch (Exception e) {
            log.warn("Ошибка при закрытии консьюмера: {}", e.getMessage());
        }

        log.info("AggregationStarter завершен");
    }
}
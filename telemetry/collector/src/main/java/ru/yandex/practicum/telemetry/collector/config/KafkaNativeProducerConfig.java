package ru.yandex.practicum.telemetry.collector.config;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.avro.specific.SpecificRecordBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.serializer.GeneralAvroSerializer;

import java.util.Properties;

@Configuration
public class KafkaNativeProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean(destroyMethod = "close")
    public Producer<String, SpecificRecordBase> nativeKafkaProducer() {
        Properties properties = new Properties();

        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");

        Serializer<String> keySerializer = new StringSerializer();
        Serializer<SpecificRecordBase> valueSerializer = new GeneralAvroSerializer();

        return new KafkaProducer<>(properties, keySerializer, valueSerializer);
    }
}
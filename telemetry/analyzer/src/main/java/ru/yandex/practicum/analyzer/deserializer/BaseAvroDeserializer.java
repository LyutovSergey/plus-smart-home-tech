package ru.yandex.practicum.analyzer.deserializer;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@Slf4j
public abstract class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DatumReader<T> reader;

    protected BaseAvroDeserializer(Schema schema) {
        this.reader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            Decoder decoder = DecoderFactory.get().binaryDecoder(input, null);
            return reader.read(null, decoder);
        } catch (IOException e) {
            log.error("Ошибка десериализации Avro-сообщения из топика {}", topic, e);
            throw new RuntimeException("Не удалось десериализовать сообщение", e);
        }
    }

    @Override
    public void close() {
    }
}
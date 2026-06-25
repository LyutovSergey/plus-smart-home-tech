package ru.yandex.practicum.telemetry.aggregator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.yandex.practicum.telemetry.aggregator.service.AggregationStarter;

@Slf4j
@SpringBootApplication
public class AggregatorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApplication.class, args);

        final AggregationStarter aggregator = context.getBean(AggregationStarter.class);
        aggregator.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                log.info("Получен сигнал завершения, останавливаем Aggregator...");
                aggregator.stop();
            }
        });
    }
}

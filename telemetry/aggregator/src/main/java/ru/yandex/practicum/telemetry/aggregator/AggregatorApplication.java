package ru.yandex.practicum.telemetry.aggregator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import ru.yandex.practicum.telemetry.aggregator.service.AggregationStarter;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@ConfigurationPropertiesScan
public class AggregatorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApplication.class, args);

        // Ломает подключение к эврике
        //  AggregationStarter aggregator = context.getBean(AggregationStarter.class);
        // aggregator.start();
    }
    @Bean
    public CommandLineRunner runKafkaAggregator(AggregationStarter aggregator) {
        return args -> {
            log.info("Контекст и веб-сервер готовы. Запускаем бесконечный цикл Кафки...");
            aggregator.start();
        };
    }
}

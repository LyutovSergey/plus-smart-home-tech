package ru.yandex.practicum.telemetry.analyzer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;
import ru.yandex.practicum.telemetry.analyzer.processor.HubEventProcessor;
import ru.yandex.practicum.telemetry.analyzer.processor.SnapshotProcessor;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Analyzer { public static void main(String[] args) {

    SpringApplication.run(Analyzer.class, args);
}

    @Bean
    public CommandLineRunner runTelemetry(HubEventProcessor hubEventProcessor,
                                          SnapshotProcessor snapshotProcessor) {
        return args -> {

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                hubEventProcessor.shutdown();
                snapshotProcessor.shutdown();
            }));

            Thread hubEventsThread = new Thread(hubEventProcessor);
            hubEventsThread.setName("HubEventHandlerThread");
            hubEventsThread.start();

            snapshotProcessor.start();
        };
    }
}
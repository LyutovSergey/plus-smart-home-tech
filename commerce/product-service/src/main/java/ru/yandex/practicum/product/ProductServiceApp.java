package ru.yandex.practicum.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProductServiceApp {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApp.class, args);
    }
}
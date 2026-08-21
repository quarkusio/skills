package com.coffeeshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.coffeeshop.common.domain")
@EnableJpaRepositories(basePackages = "com.coffeeshop.counter.store")
public class CoffeeshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoffeeshopApplication.class, args);
    }
}

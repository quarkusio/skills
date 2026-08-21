package com.coffeeshop.common.domain;

import com.coffeeshop.counter.store.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = Replace.NONE) // use the container, not H2
@EntityScan(basePackages = "com.coffeeshop.common.domain")
@EnableJpaRepositories(basePackages = "com.coffeeshop.counter.store")
class OrderRepositoryIT {

    // JUnit/Testcontainers manages start/stop for you
    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("coffee_test")
            .withUsername("test")
            .withPassword("test");

    // Wire Spring to the running container
    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    @Autowired
    OrderRepository orderRepository;

    @Test
    void persist_and_load_order() {
        Order order = new Order(); // protected no-args ctor visible because same package
        order.setOrderId("order-xyz");
        order.setOrderSource(OrderSource.WEB);
        order.setLocation(Location.ATLANTA);
        order.setTimestamp(Instant.now());
        order.setOrderStatus(OrderStatus.IN_PROGRESS);

        LineItem li = new LineItem(
            Item.ESPRESSO, "Bob", new BigDecimal("4.00"),
            ItemStatus.IN_PROGRESS, order
        );
        order.addBaristaLineItem(li);

        orderRepository.save(order);

        var found = orderRepository.findById("order-xyz").orElseThrow();
        assertThat(found.getOrderId()).isEqualTo("order-xyz");
        assertThat(found.getBaristaLineItems()).isPresent();
        assertThat(found.getBaristaLineItems().get()).hasSize(1);
    }
}

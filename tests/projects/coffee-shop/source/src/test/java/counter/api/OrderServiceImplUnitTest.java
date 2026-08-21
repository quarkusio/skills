package com.coffeeshop.counter.api;

import com.coffeeshop.common.commands.CommandItem;
import com.coffeeshop.common.commands.PlaceOrderCommand;
import com.coffeeshop.common.domain.Item;
import com.coffeeshop.common.domain.Location;
import com.coffeeshop.common.domain.OrderSource;
import com.coffeeshop.common.domain.Order;
import com.coffeeshop.counter.store.OrderRepository;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceImplTest {

    @Test
    @DisplayName("onOrderIn -> sends ProducerRecords to WEB_UPDATES and BARISTA_IN")
    void testOnOrderInPublishesEvents() {
        // Mocks
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate = mock(KafkaTemplate.class);
        OrderRepository orderRepository = mock(OrderRepository.class);

        // Save returns the same entity
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // IMPORTANT: stub the ProducerRecord overload
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        // SUT
        OrderServiceImpl service = new OrderServiceImpl(kafkaTemplate, orderRepository);

        // Barista item so we exercise both topics
        List<CommandItem> baristaItems = List.of(
                new CommandItem(Item.ESPRESSO, "Alex", new BigDecimal("4.00"))
        );

        PlaceOrderCommand cmd = new PlaceOrderCommand(
                "order-123",
                OrderSource.WEB,
                Location.ATLANTA,
                null,                  // loyaltyMemberId
                baristaItems,          // BARISTA
                null,                  // no KITCHEN
                Instant.now()
        );

        // Act
        service.onOrderIn(cmd);

        // Assert: capture ProducerRecords that were sent
        @SuppressWarnings("unchecked")
        ArgumentCaptor<ProducerRecord<String, String>> prCaptor =
                ArgumentCaptor.forClass((Class) ProducerRecord.class);

        verify(kafkaTemplate, atLeastOnce()).send(prCaptor.capture());
        verify(orderRepository).save(any(Order.class));

        // Inspect captures
        var records = prCaptor.getAllValues();
        assertThat(records).isNotEmpty();

        boolean sawWebUpdates = records.stream()
                .anyMatch(r -> "WEB_UPDATES".equals(r.topic()) &&
                               "order-123".equals(r.key()) &&
                               r.value() != null && !r.value().isBlank());

        boolean sawBaristaIn = records.stream()
                .anyMatch(r -> "BARISTA_IN".equals(r.topic()) &&
                               "order-123".equals(r.key()) &&
                               r.value() != null && !r.value().isBlank());

        // We didn't send KITCHEN_IN in this scenario
        boolean sawKitchenIn = records.stream()
                .anyMatch(r -> "KITCHEN_IN".equals(r.topic()));

        assertThat(sawWebUpdates).as("WEB_UPDATES was produced").isTrue();
        assertThat(sawBaristaIn).as("BARISTA_IN was produced").isTrue();
        assertThat(sawKitchenIn).as("KITCHEN_IN should not be produced").isFalse();
    }
}

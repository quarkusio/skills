package com.coffeeshop.counter.messaging;

import com.coffeeshop.common.utils.JsonUtil;
import com.coffeeshop.common.valueobjects.OrderUp;
import com.coffeeshop.counter.api.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import static com.coffeeshop.common.messaging.Topics.ORDERS_UP;

@Component
public class OrderUpListener {

    private static final Logger log = LoggerFactory.getLogger(OrderUpListener.class);

    private final OrderService orderService;

    public OrderUpListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @KafkaListener(topics = ORDERS_UP, groupId = "counter-service")
    public void consumeOrderUp(String json) {
        log.info("Kafka consumed ORDERS_UP: {}", json);

        // Reuse the same service method that expects Message<String>
        Message<String> msg = MessageBuilder.withPayload(json).build();
        orderService.onOrderUp(msg);
    }
}

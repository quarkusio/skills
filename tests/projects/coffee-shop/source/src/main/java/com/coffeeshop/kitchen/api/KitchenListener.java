package com.coffeeshop.kitchen.api;

import com.coffeeshop.common.utils.JsonUtil;
import com.coffeeshop.common.valueobjects.OrderIn;
import com.coffeeshop.common.valueobjects.OrderUp;
import static com.coffeeshop.common.messaging.Topics.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class KitchenListener {

    private static final Logger log = LoggerFactory.getLogger(KitchenListener.class);

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KitchenListener(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = KITCHEN_IN, groupId = "kitchen-service")
    public void onKitchenIn(ConsumerRecord<String, String> record, @Payload String json) {
        log.info("KITCHEN_IN received: key={}, value={}", record.key(), json);

        // Parse ticket
        OrderIn in = JsonUtil.fromJson(json, OrderIn.class);

        // Simulate kitchen prep
        try {
            Thread.sleep(300); // quick demo delay
        } catch (InterruptedException ignored) {}

        // Build OrderUp
        OrderUp up = new OrderUp(
                in.orderId,
                in.itemId,
                in.item,
                in.name,
                Instant.now(),
                "KitchenBot"
        );

        String payload = JsonUtil.toJson(up);
        String key = in.orderId != null ? in.orderId : null;

        kafkaTemplate.send(ORDERS_UP, key, payload);
        log.info("Published ORDERS_UP: key={}, payload={}", key, payload);
    }
}

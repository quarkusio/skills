package com.coffeeshop.barista.api;

import com.coffeeshop.common.utils.JsonUtil;
import com.coffeeshop.common.valueobjects.OrderIn;
import com.coffeeshop.common.valueobjects.OrderUp;

import static com.coffeeshop.common.messaging.Topics.*;

import java.time.Instant;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class BaristaListener {

    private static final Logger log = LoggerFactory.getLogger(BaristaListener.class);

    private final KafkaTemplate<String, String> kafkaTemplate;


    public BaristaListener(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @KafkaListener(topics = BARISTA_IN, groupId = "barista-service")
    public void onBaristaIn(ConsumerRecord<String, String> record, @Payload String json) {
        log.info("BARISTA_IN received: key={}, value={}", record.key(), json);

        // Parse OrderIn
        OrderIn in = JsonUtil.fromJson(json, OrderIn.class);

        // Simulate making the drink (optional delay)
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
                "BaristaBot"      // madeBy
        );

        String payload = JsonUtil.toJson(up);

        // Use orderId as key for partition-affinity (ordering per order)
        String key = in.orderId != null ? in.orderId : null;

        kafkaTemplate.send(ORDERS_UP, key, payload);
        log.info("Published ORDERS_UP: key={}, payload={}", key, payload);
    }
}

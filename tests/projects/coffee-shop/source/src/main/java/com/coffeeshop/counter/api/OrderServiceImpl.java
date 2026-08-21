package com.coffeeshop.counter.api;

import com.coffeeshop.common.commands.PlaceOrderCommand;
import com.coffeeshop.common.domain.Order;
import com.coffeeshop.common.events.OrderEventResult;
import com.coffeeshop.common.utils.JsonUtil;
import com.coffeeshop.counter.store.OrderRepository;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.coffeeshop.common.messaging.Topics.*;
import static com.coffeeshop.common.utils.JsonUtil.fromJsonToOrderUp; // if you exposed this
// or use JsonUtil directly.

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OrderRepository orderRepository;

    public OrderServiceImpl(KafkaTemplate<String, String> kafkaTemplate,
                            OrderRepository orderRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional
    public OrderEventResult onOrderIn(final PlaceOrderCommand placeOrderCommand) {
        LOGGER.debug("PlaceOrderCommand received: {}", placeOrderCommand);

        // Build domain + tickets/updates from command
        OrderEventResult orderEventResult = Order.from(placeOrderCommand);

        // Persist order
        Order order = orderEventResult.getOrder();
        orderRepository.save(order); // don’t use the return value
        LOGGER.debug("Persisted order {}", order.getOrderId());

        // Publish web updates (WEB_UPDATES)
        if (orderEventResult.getOrderUpdates() != null) {
            orderEventResult.getOrderUpdates().forEach(orderUpdate -> {
                String payload = JsonUtil.toJson(orderUpdate);
                try {
                    kafkaTemplate.send(new ProducerRecord<>(WEB_UPDATES, order.getOrderId(), payload));
                    LOGGER.debug("sent WEB_UPDATES: {}", payload);
                } catch (Exception e) {
                    LOGGER.warn("Could not send WEB_UPDATES to Kafka: {}", e.getMessage());
                }
            });
        }

        // Publish barista tickets (BARISTA_IN)
        orderEventResult.getBaristaTickets().ifPresent(list -> list.forEach(ticket -> {
            String payload = JsonUtil.toJson(ticket);
            try {
                kafkaTemplate.send(new ProducerRecord<>(BARISTA_IN, order.getOrderId(), payload));
                LOGGER.debug("sent BARISTA_IN: {}", payload);
            } catch (Exception e) {
                LOGGER.warn("Could not send BARISTA_IN to Kafka: {}", e.getMessage());
            }
        }));

        // Publish kitchen tickets (KITCHEN_IN))
        orderEventResult.getKitchenTickets().ifPresent(list -> list.forEach(ticket -> {
            String payload = JsonUtil.toJson(ticket);
            try {
                kafkaTemplate.send(new ProducerRecord<>(KITCHEN_IN, order.getOrderId(), payload));
                LOGGER.debug("sent KITCHEN_IN: {}", payload);
            } catch (Exception e) {
                LOGGER.warn("Could not send KITCHEN_IN to Kafka: {}", e.getMessage());
            }
        }));

        return orderEventResult;
    }

    @Override
    @Transactional
    public void onOrderUp(final Message<String> message) {
        String json = message.getPayload();
        LOGGER.debug("ORDERS_UP message received via REST bridge: {}", json);

        var orderUp = fromJsonToOrderUp(json); // or JsonUtil.fromJsonToOrderUp(json)
        Order order = orderRepository.findById(orderUp.orderId).orElse(null);

        if (order == null) {
            LOGGER.warn("Order {} not found for OrderUp; ignoring.", orderUp.orderId);
            return;
        }

        OrderEventResult result = order.apply(orderUp);
        orderRepository.save(order);

        if (result.getOrderUpdates() != null) {
            result.getOrderUpdates().forEach(update -> {
                String payload = JsonUtil.toJson(update);
                kafkaTemplate.send(WEB_UPDATES, payload);
                LOGGER.debug("sent WEB_UPDATES after apply: {}", payload);
            });
        }
    }
}

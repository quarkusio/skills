package com.coffeeshop.kitchen.api;

import com.coffeeshop.common.utils.JsonUtil;
import com.coffeeshop.common.valueobjects.OrderUp;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import static com.coffeeshop.common.messaging.Topics.KITCHEN_IN;
import static com.coffeeshop.common.messaging.Topics.ORDERS_UP;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class KitchenListenerTest {

    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    KitchenListener listener;

    @Captor ArgumentCaptor<String> topicCaptor;
    @Captor ArgumentCaptor<String> keyCaptor;
    @Captor ArgumentCaptor<String> valueCaptor;

    @Test
    @DisplayName("onKitchenIn -> publishes OrderUp JSON to ORDERS_UP with orderId as key (send(topic,key,value))")
    void onKitchenIn_publishesOrderUp() {
        // Arrange
        String orderId = "order-456";
        String itemId  = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
        String jsonIn = """
            {
              "orderId": "%s",
              "item": "CROISSANT",
              "name": "Taylor",
              "itemId": "%s",
              "timeIn": 1756789058.77
            }
            """.formatted(orderId, itemId);

        ConsumerRecord<String, String> consumerRecord =
                new ConsumerRecord<>(KITCHEN_IN, 0, 0L, orderId, jsonIn);

        // Act
        listener.onKitchenIn(consumerRecord, jsonIn);

        // Assert the 3-arg send(topic, key, value)
        verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

        assertThat(topicCaptor.getValue()).isEqualTo(ORDERS_UP);
        assertThat(keyCaptor.getValue()).isEqualTo(orderId);

        // Validate payload by deserializing to OrderUp
        OrderUp up = JsonUtil.fromJson(valueCaptor.getValue(), OrderUp.class);
        assertThat(up.orderId).isEqualTo(orderId);
        assertThat(up.itemId).isEqualTo(itemId);
        assertThat(up.item.name()).isEqualTo("CROISSANT");
        assertThat(up.name).isEqualTo("Taylor");
        // Expect whatever your KitchenListener sets (e.g., "KitchenBot")
        assertThat(up.madeBy).isNotBlank();
        assertThat(up.timeUp).isNotNull();
    }
}

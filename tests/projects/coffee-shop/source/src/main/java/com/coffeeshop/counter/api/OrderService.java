package com.coffeeshop.counter.api;

import com.coffeeshop.common.commands.PlaceOrderCommand;
import com.coffeeshop.common.events.OrderEventResult;
import org.springframework.messaging.Message;

public interface OrderService {
    OrderEventResult onOrderIn(PlaceOrderCommand placeOrderCommand);
    void onOrderUp(Message<String> message);
}

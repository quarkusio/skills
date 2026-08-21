package com.coffeeshop.web.api;

import com.coffeeshop.common.commands.PlaceOrderCommand;
import com.coffeeshop.common.domain.Order;
import com.coffeeshop.common.events.OrderEventResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coffeeshop")
public class CoffeeshopApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoffeeshopApiController.class);

    /**
     * Accept a PlaceOrderCommand and return the computed OrderEventResult.
     * No DB or Kafka yet; purely domain logic from the common module.
     */

     /**
      * NO @Transactional here - it will move to service layer
      * 
      * @return
      */


    @PostMapping("/order")
    public ResponseEntity<OrderEventResult> placeOrder(@RequestBody PlaceOrderCommand placeOrderCommand) {
        LOGGER.info("PlaceOrderCommand received: {}", placeOrderCommand);
        OrderEventResult result = Order.from(placeOrderCommand);
        return ResponseEntity.accepted().body(result);
    }

    /**
     * Simple message endpoint to verify wiring. For now, just logs the message.
     * You can switch this to publish via Spring's ApplicationEventPublisher or Kafka later.
     */
    @PostMapping("/message")
    public ResponseEntity<Void> sendMessage(@RequestBody String message) {
        LOGGER.debug("received message: {}", message);
        // TODO: replace with Spring event publisher or Kafka template after migration
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/health")
    public String health() {
        return "web-service OK";
    }
}


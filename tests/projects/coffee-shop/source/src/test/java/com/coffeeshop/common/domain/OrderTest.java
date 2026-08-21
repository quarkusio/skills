package com.coffeeshop.common.domain;

import com.coffeeshop.common.commands.CommandItem;
import com.coffeeshop.common.commands.PlaceOrderCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void from_buildsTicketsAndUpdates_forBaristaAndKitchen() {
        var barista = List.of(new CommandItem(Item.ESPRESSO, "Alex", new BigDecimal("4.00")));
        var kitchen = List.of(new CommandItem(Item.CROISSANT, "Sam", new BigDecimal("3.00")));

        var cmd = new PlaceOrderCommand(
                "order-abc",
                OrderSource.WEB,
                Location.ATLANTA,
                null,          // loyaltyMemberId
                barista,       // baristaItems (nullable allowed)
                kitchen,       // kitchenItems (nullable allowed)
                Instant.now()
        );

        var result = Order.from(cmd);

        assertThat(result.getOrder()).isNotNull();
        assertThat(result.getOrder().getOrderId()).isEqualTo("order-abc");
        assertThat(result.getBaristaTickets()).isPresent();
        assertThat(result.getBaristaTickets().get()).hasSize(1);
        assertThat(result.getKitchenTickets()).isPresent();
        assertThat(result.getKitchenTickets().get()).hasSize(1);
        assertThat(result.getOrderUpdates()).isNotNull().isNotEmpty();
    }
}

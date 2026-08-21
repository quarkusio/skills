package com.coffeeshop.common.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.coffeeshop.common.domain.Item;

import java.time.Instant;

/**
 * A value object encapsulating the information sent to Barista and Kitchen
 */
public class OrderIn {

    public final String orderId;

    public final String itemId;

    public final Item item;

    public final String name;

    public final Instant timeIn;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderIn(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("lineItemId") String itemId,
            @JsonProperty("item") Item item,
            @JsonProperty("name") String name) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.item = item;
        this.name = name;
        this.timeIn = Instant.now();
    }

    @Override
    public String toString() {
        return "OrderTicket{" +
                "orderId='" + orderId + '\'' +
                ", lineItemId='" + itemId + '\'' +
                ", item=" + item +
                ", name='" + name + '\'' +
                ", timestamp=" + timeIn +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderIn that = (OrderIn) o;

        if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) return false;
        if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) return false;
        if (item != that.item) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        return timeIn != null ? timeIn.equals(that.timeIn) : that.timeIn == null;
    }

    @Override
    public int hashCode() {
        int result = orderId != null ? orderId.hashCode() : 0;
        result = 31 * result + (itemId != null ? itemId.hashCode() : 0);
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (timeIn != null ? timeIn.hashCode() : 0);
        return result;
    }
}

package com.coffeeshop.common.valueobjects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.coffeeshop.common.domain.Item;

import java.time.Instant;

public class OrderUp extends OrderIn{

    public final String madeBy;

    public final Instant timeUp;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public OrderUp(
            @JsonProperty("orderId") String orderId,
            @JsonProperty("lineItemId") String lineItemId,
            @JsonProperty("item") Item item,
            @JsonProperty("name") String name,
            @JsonProperty("timestamp") Instant timeUp,
            @JsonProperty("madeBy") String madeBy) {
        super(orderId, lineItemId, item, name);
        this.timeUp = timeUp;
        this.madeBy = madeBy;
    }

    @Override
    public String toString() {
        return "OrderUp{" +
                "orderId='" + orderId + '\'' +
                ", lineItemId='" + itemId + '\'' +
                ", item=" + item +
                ", name='" + name + '\'' +
                ", timestamp=" + timeUp +
                ", madeBy='" + madeBy + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        OrderUp orderUp = (OrderUp) o;

        return madeBy != null ? madeBy.equals(orderUp.madeBy) : orderUp.madeBy == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (madeBy != null ? madeBy.hashCode() : 0);
        return result;
    }
}

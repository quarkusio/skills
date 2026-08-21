package com.coffeeshop.common.valueobjects;

import com.coffeeshop.common.domain.Item;
import com.coffeeshop.common.domain.OrderStatus;

public class OrderUpdate {

    public final String orderId;

    public final String itemId;

    public final String name;

    public final Item item;

    public final OrderStatus status;

    public final String madeBy;

    public OrderUpdate(final String orderId, final String itemId, final String name, final Item item, final OrderStatus status) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.name = name;
        this.item = item;
        this.status = status;
        this.madeBy = null;
    }

    public OrderUpdate(final String orderId, final String itemId, final String name, final Item item, final OrderStatus status, final String madeBy) {
        this.orderId = orderId;
        this.itemId = itemId;
        this.name = name;
        this.item = item;
        this.status = status;
        this.madeBy = madeBy;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("OrderUpdate{");
        sb.append("orderId='").append(orderId).append('\'');
        sb.append(", itemId='").append(itemId).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", item=").append(item);
        sb.append(", status=").append(status);
        sb.append(", madeBy='").append(madeBy).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        OrderUpdate that = (OrderUpdate) o;

        if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) return false;
        if (itemId != null ? !itemId.equals(that.itemId) : that.itemId != null) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (item != that.item) return false;
        if (status != that.status) return false;
        return madeBy != null ? madeBy.equals(that.madeBy) : that.madeBy == null;
    }

    @Override
    public int hashCode() {
        int result = orderId != null ? orderId.hashCode() : 0;
        result = 31 * result + (itemId != null ? itemId.hashCode() : 0);
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (item != null ? item.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (madeBy != null ? madeBy.hashCode() : 0);
        return result;
    }


}

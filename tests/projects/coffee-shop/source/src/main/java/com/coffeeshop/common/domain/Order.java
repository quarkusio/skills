package com.coffeeshop.common.domain;

// was import io.quarkuscoffeeshop.coffeeshop.counter.domain.OrderEventResult;
import com.coffeeshop.common.events.OrderEventResult;
import com.coffeeshop.common.commands.PlaceOrderCommand;
import com.coffeeshop.common.valueobjects.OrderIn;
import com.coffeeshop.common.valueobjects.OrderUp;
import com.coffeeshop.common.valueobjects.OrderUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Entity
@Table(name = "Orders")
public class Order  {

    @Transient
    static Logger LOGGER = LoggerFactory.getLogger(Order.class);

    @Id
    @Column(nullable = false, unique = true, name = "order_id")
    private String orderId;

    @Enumerated(EnumType.STRING)
    private OrderSource orderSource;

    private String loyaltyMemberId;

    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    private Location location;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "order", cascade = CascadeType.ALL)
    private List<LineItem> baristaLineItems;

    @OneToMany(fetch = FetchType.EAGER, mappedBy = "order", cascade = CascadeType.ALL)
    private List<LineItem> kitchenLineItems;

    protected Order() {

    }
   
    private Order(String orderId) {
        this.orderId = orderId;
        this.timestamp = Instant.now();
    }

    /** remove fromAsync() method 
     * 
    public static Uni<OrderEventResult> fromAsync(final PlaceOrderCommand placeOrderCommand) {
        return Uni.createFrom().item(from(placeOrderCommand));
    }
    */
    
/**
 * Creates a new Order and the corresponding event bundle from a PlaceOrderCommand.
 * Updated to work with plain fields in PlaceOrderCommand (no Optional fields).
 */
public static OrderEventResult from(final PlaceOrderCommand placeOrderCommand) {
    // Build the Order
    Order order = new Order(placeOrderCommand.getId());
    order.setOrderSource(placeOrderCommand.getOrderSource());
    order.setLocation(placeOrderCommand.getLocation());
    order.setTimestamp(placeOrderCommand.getTimestamp());
    order.setOrderStatus(OrderStatus.IN_PROGRESS);

    // Loyalty ID may be null
    if (placeOrderCommand.getLoyaltyMemberId() != null && !placeOrderCommand.getLoyaltyMemberId().isBlank()) {
        order.setLoyaltyMemberId(placeOrderCommand.getLoyaltyMemberId());
    }

    // Prepare the return envelope
    OrderEventResult orderEventResult = new OrderEventResult();
    orderEventResult.setOrder(order); // set early, safe no-op

    // BARISTA items
    if (placeOrderCommand.getBaristaItems() != null && !placeOrderCommand.getBaristaItems().isEmpty()) {
        LOGGER.debug("Adding {} barista items", placeOrderCommand.getBaristaItems().size());

        placeOrderCommand.getBaristaItems().forEach(cmdItem -> {
            LineItem li = new LineItem(cmdItem.item, cmdItem.name, cmdItem.price, ItemStatus.IN_PROGRESS, order);
            order.addBaristaLineItem(li);

            // ticket for barista
            orderEventResult.addBaristaTicket(new OrderIn(
                    order.getOrderId(),
                    li.getItemId(),
                    li.getItem(),
                    li.getName()
            ));

            // order update
            orderEventResult.addUpdate(new OrderUpdate(
                    order.getOrderId(),
                    li.getItemId(),
                    li.getName(),
                    li.getItem(),
                    OrderStatus.IN_PROGRESS
            ));
        });
    }

    // KITCHEN items
    if (placeOrderCommand.getKitchenItems() != null && !placeOrderCommand.getKitchenItems().isEmpty()) {
        LOGGER.debug("Adding {} kitchen items", placeOrderCommand.getKitchenItems().size());

        placeOrderCommand.getKitchenItems().forEach(cmdItem -> {
            LineItem li = new LineItem(cmdItem.item, cmdItem.name, cmdItem.price, ItemStatus.IN_PROGRESS, order);
            order.addKitchenLineItem(li);

            // ticket for kitchen
            orderEventResult.addKitchenTicket(new OrderIn(
                    order.getOrderId(),
                    li.getItemId(),
                    li.getItem(),
                    li.getName()
            ));

            // order update
            orderEventResult.addUpdate(new OrderUpdate(
                    order.getOrderId(),
                    li.getItemId(),
                    li.getName(),
                    li.getItem(),
                    OrderStatus.IN_PROGRESS
            ));
        });
    }

    LOGGER.debug("Order.from -> {}", orderEventResult);
    return orderEventResult;
}

    public OrderEventResult apply(final OrderUp orderUp) {

        LOGGER.debug("applying orderUp: {}", orderUp);
        OrderEventResult orderEventResult = new OrderEventResult();

        orderEventResult.addUpdate(new OrderUpdate(
                orderUp.orderId,
                orderUp.itemId,
                orderUp.name,
                orderUp.item,
                OrderStatus.FULFILLED,
                orderUp.madeBy));

        // loop through barista tickets and update this line item
        if (this.getBaristaLineItems().isPresent()) {
            this.getBaristaLineItems().get().stream().forEach(baristaLineItem -> {
                if (baristaLineItem.getItemId().equals(orderUp.itemId)) {
                    baristaLineItem.setItemStatus(ItemStatus.FULFILLED);
                    orderEventResult.addUpdate(new OrderUpdate(orderUp.orderId, orderUp.itemId, orderUp.name, orderUp.item, OrderStatus.FULFILLED, orderUp.madeBy));
                }
            });
        }
        if (this.getKitchenLineItems().isPresent()) {
            this.getKitchenLineItems().get().stream().forEach(kitchenLineItem -> {
                if (kitchenLineItem.getItemId().equals(orderUp.itemId)) {
                    kitchenLineItem.setItemStatus(ItemStatus.FULFILLED);
                    orderEventResult.addUpdate(new OrderUpdate(orderUp.orderId, orderUp.itemId, orderUp.name, orderUp.item, OrderStatus.FULFILLED, orderUp.madeBy));
                }
            });
        }

        // if there are both barista and kitchen items concatenate them before checking status
        if (this.getBaristaLineItems().isPresent() && this.getKitchenLineItems().isPresent()) {
            // check the status of the Order itself and update if necessary
            if(Stream.concat(this.baristaLineItems.stream(), this.kitchenLineItems.stream())
                    .allMatch(lineItem -> {
                        return lineItem.getItemStatus().equals(ItemStatus.FULFILLED);
                    })){
                this.setOrderStatus(OrderStatus.FULFILLED);
            };
        }else if (this.getBaristaLineItems().isPresent()) {
            if(this.baristaLineItems.stream()
                    .allMatch(lineItem -> {
                        return lineItem.getItemStatus().equals(ItemStatus.FULFILLED);
                    })){
                this.setOrderStatus(OrderStatus.FULFILLED);
            };
        }else if (this.getKitchenLineItems().isPresent()) {
            if(this.kitchenLineItems.stream()
                    .allMatch(lineItem -> {
                        return lineItem.getItemStatus().equals(ItemStatus.FULFILLED);
                    })){
                this.setOrderStatus(OrderStatus.FULFILLED);
            };
        }

        LOGGER.debug("apply OrderUp event complete: {}", this);
        orderEventResult.setOrder(this);

        return orderEventResult;
    }

    /**
     * Convenience method to prevent Null Pointer Exceptions
     *
     * @param lineItem
     */
    public void addBaristaLineItem(LineItem lineItem) {
        if (this.baristaLineItems == null) {
            this.baristaLineItems = new ArrayList<>();
        }
        lineItem.setOrder(this);
        this.baristaLineItems.add(lineItem);
    }

    /**
     * Convenience method to prevent Null Pointer Exceptions
     *
     * @param lineItem
     */
    public void addKitchenLineItem(LineItem lineItem) {
        if (this.kitchenLineItems == null) {
            this.kitchenLineItems = new ArrayList<>();
        }
        lineItem.setOrder(this);
        this.kitchenLineItems.add(lineItem);
    }


    /**
     * Not all Orders come from Loyalty Members so this returns an Optional<String> of the Loyalty Member's Id
     *
     * @return Optional<String>
     */
    public Optional<String> getLoyaltyMemberId() {
        return Optional.ofNullable(loyaltyMemberId);
    }

    /**
     * Most but not all Orders have barista items so this returns an Optional<List<LineItem>
     *
     * @return Optional<String>
     */
    public Optional<List<LineItem>> getBaristaLineItems() {
        return Optional.ofNullable(baristaLineItems);
    }

    /**
     * Many orders do not contain kitchen items so this returns an Optional<List<LineItem>
     *
     * @return Optional<String>
     */
    public Optional<List<LineItem>> getKitchenLineItems() {
        return Optional.ofNullable(kitchenLineItems);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Order{");
        sb.append("orderId='").append(orderId).append('\'');
        sb.append(", orderSource=").append(orderSource);
        sb.append(", loyaltyMemberId='").append(loyaltyMemberId).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", orderStatus=").append(orderStatus);
        sb.append(", location=").append(location);
        sb.append(", baristaLineItems=[");
        if (getBaristaLineItems().isPresent()) {
            sb.append(baristaLineItems.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }else{
            sb.append("]");
        }
        sb.append(", kitchenLineItems=[");
        if (getKitchenLineItems().isPresent()) {
            sb.append(kitchenLineItems.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }else{
            sb.append("]");
        }
        sb.append('}');
        return sb.toString();
    }

    // Generated
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Order order = (Order) o;

        if (orderId != null ? !orderId.equals(order.orderId) : order.orderId != null) return false;
        if (orderSource != order.orderSource) return false;
        if (loyaltyMemberId != null ? !loyaltyMemberId.equals(order.loyaltyMemberId) : order.loyaltyMemberId != null)
            return false;
        if (timestamp != null ? !timestamp.equals(order.timestamp) : order.timestamp != null) return false;
        if (orderStatus != order.orderStatus) return false;
        if (location != order.location) return false;
        if (baristaLineItems != null ? !baristaLineItems.equals(order.baristaLineItems) : order.baristaLineItems != null)
            return false;
        return kitchenLineItems != null ? kitchenLineItems.equals(order.kitchenLineItems) : order.kitchenLineItems == null;
    }

    @Override
    public int hashCode() {
        int result = orderId != null ? orderId.hashCode() : 0;
        result = 31 * result + (orderSource != null ? orderSource.hashCode() : 0);
        result = 31 * result + (loyaltyMemberId != null ? loyaltyMemberId.hashCode() : 0);
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (orderStatus != null ? orderStatus.hashCode() : 0);
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (baristaLineItems != null ? baristaLineItems.hashCode() : 0);
        result = 31 * result + (kitchenLineItems != null ? kitchenLineItems.hashCode() : 0);
        return result;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public OrderSource getOrderSource() {
        return orderSource;
    }

    public void setOrderSource(OrderSource orderSource) {
        this.orderSource = orderSource;
    }

    public void setLoyaltyMemberId(String loyaltyMemberId) {
        this.loyaltyMemberId = loyaltyMemberId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setBaristaLineItems(List<LineItem> baristaLineLineItems) {
        this.baristaLineItems = baristaLineLineItems;
    }

    public void setKitchenLineItems(List<LineItem> kitchenLineLineItems) {
        this.kitchenLineItems = kitchenLineLineItems;
    }
}

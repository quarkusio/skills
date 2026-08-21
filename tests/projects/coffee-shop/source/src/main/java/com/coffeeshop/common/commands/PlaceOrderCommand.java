package com.coffeeshop.common.commands;

import com.coffeeshop.common.domain.Location;
import com.coffeeshop.common.domain.OrderSource;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class PlaceOrderCommand {

    private final CommandType commandType = CommandType.PLACE_ORDER;

    @NotBlank
    private final String id;

    @NotNull
    private final OrderSource orderSource;

    @NotNull
    private final Location location;

    @Size(max = 100)
    private final String loyaltyMemberId;   // may be null

    // may be null
    private final List<CommandItem> baristaItems;

    // may be null
    private final List<CommandItem> kitchenItems;

    @NotNull
    private final Instant timestamp;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public PlaceOrderCommand(
            @JsonProperty("id") String id,
            @JsonProperty("orderSource") OrderSource orderSource,
            @JsonProperty("location") Location location,                 // <— name matches JSON
            @JsonProperty("loyaltyMemberId") String loyaltyMemberId,     // <— plain String (nullable)
            @JsonProperty("baristaItems") List<CommandItem> baristaItems, // <— plain List (nullable)
            @JsonProperty("kitchenItems") List<CommandItem> kitchenItems, // <— plain List (nullable)
            @JsonProperty("timestamp") Instant timestamp                  // <— Instant in JSON
    ) {
        this.id = Objects.requireNonNull(id, "id");
        this.orderSource = Objects.requireNonNull(orderSource, "orderSource");
        this.location = Objects.requireNonNull(location, "location");
        this.loyaltyMemberId = loyaltyMemberId;     // can be null
        this.baristaItems = baristaItems;           // can be null
        this.kitchenItems = kitchenItems;           // can be null
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public CommandType getCommandType() { return commandType; }
    public String getId() { return id; }
    public OrderSource getOrderSource() { return orderSource; }
    public Location getLocation() { return location; }
    public Instant getTimestamp() { return timestamp; }


    // JSON-friendly getters (no Optional)
    public String getLoyaltyMemberId() { 
        return loyaltyMemberId; 
    }

    public List<CommandItem> getBaristaItems() { 
        return baristaItems; 
    }

    public List<CommandItem> getKitchenItems() { 
        return kitchenItems; 
    }

    // If you still want Optional helpers for non-JSON use, add extra methods:
    public java.util.Optional<String> loyaltyMemberIdOpt() {
        return java.util.Optional.ofNullable(loyaltyMemberId);
    }
    public java.util.Optional<java.util.List<CommandItem>> baristaItemsOpt() {
        return java.util.Optional.ofNullable(baristaItems);
    }
    public java.util.Optional<java.util.List<CommandItem>> kitchenItemsOpt() {
        return java.util.Optional.ofNullable(kitchenItems);
}


    @Override
    public String toString() {
        return "PlaceOrderCommand{" +
                "id='" + id + '\'' +
                ", orderSource=" + orderSource +
                ", location=" + location +
                ", loyaltyMemberId='" + loyaltyMemberId + '\'' +
                ", baristaItems=" + baristaItems +
                ", kitchenItems=" + kitchenItems +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlaceOrderCommand that)) return false;
        return Objects.equals(id, that.id) &&
                orderSource == that.orderSource &&
                location == that.location &&
                Objects.equals(loyaltyMemberId, that.loyaltyMemberId) &&
                Objects.equals(baristaItems, that.baristaItems) &&
                Objects.equals(kitchenItems, that.kitchenItems) &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, orderSource, location, loyaltyMemberId, baristaItems, kitchenItems, timestamp);
    }
}

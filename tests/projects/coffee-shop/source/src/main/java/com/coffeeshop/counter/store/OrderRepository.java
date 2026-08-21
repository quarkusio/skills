package com.coffeeshop.counter.store;

import com.coffeeshop.common.domain.Order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
    //following lines added after postgres is introduced
    
    // Optional: eager load line items when fetching one order
    @EntityGraph(attributePaths = {"baristaLineItems", "kitchenLineItems"})
    Order findWithItemsByOrderId(String orderId);


}

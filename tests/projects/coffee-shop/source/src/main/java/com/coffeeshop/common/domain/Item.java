package com.coffeeshop.common.domain;

import java.math.BigDecimal;

public enum Item {

    //Beverages
    CAPPUCCINO(BigDecimal.valueOf(4.50)), COFFEE_BLACK(BigDecimal.valueOf(3.00)), COFFEE_WITH_ROOM(BigDecimal.valueOf(3.00)), ESPRESSO(BigDecimal.valueOf(3.50)), ESPRESSO_DOUBLE(BigDecimal.valueOf(4.50)), LATTE(BigDecimal.valueOf(4.50)),

    //Food
    CAKEPOP(BigDecimal.valueOf(2.50)), CROISSANT(BigDecimal.valueOf(3.25)), MUFFIN(BigDecimal.valueOf(3.00)), CROISSANT_CHOCOLATE(BigDecimal.valueOf(3.50));

    private BigDecimal price;

    public BigDecimal getPrice() {
        return this.price;
    }

    private Item(BigDecimal price) {
        this.price = price;
    }
}

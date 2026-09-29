package com.sindhu.pipeline;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderRequestTest {

    @Test
    void validRequestHasNoErrors() {
        var request = new OrderRequest("C-100", "Brake Pads", 2, new BigDecimal("49.99"));
        assertTrue(request.validate().isEmpty());
    }

    @Test
    void invalidRequestReportsEveryProblem() {
        var request = new OrderRequest(" ", null, 0, BigDecimal.ZERO);
        assertEquals(4, request.validate().size());
    }

    @Test
    void orderTotalIsQuantityTimesUnitPrice() {
        var order = Order.from(new OrderRequest("C-100", "Brake Pads", 3, new BigDecimal("10.50")));
        assertEquals(new BigDecimal("31.50"), order.total());
    }
}

package com.sindhu.pipeline;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Body of POST /orders. */
public record OrderRequest(String customerId, String product, int quantity, BigDecimal unitPrice) {

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (customerId == null || customerId.isBlank()) {
            errors.add("customerId is required");
        }
        if (product == null || product.isBlank()) {
            errors.add("product is required");
        }
        if (quantity < 1) {
            errors.add("quantity must be at least 1");
        }
        if (unitPrice == null || unitPrice.signum() <= 0) {
            errors.add("unitPrice must be greater than 0");
        }
        return errors;
    }
}

package com.sindhu.pipeline;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** The message that travels through SQS and is stored in DynamoDB. */
public record Order(
        String orderId,
        String customerId,
        String product,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal total,
        String createdAt) {

    public static Order from(OrderRequest request) {
        return new Order(
                UUID.randomUUID().toString(),
                request.customerId(),
                request.product(),
                request.quantity(),
                request.unitPrice(),
                request.unitPrice().multiply(BigDecimal.valueOf(request.quantity())),
                Instant.now().toString());
    }
}

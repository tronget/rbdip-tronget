package com.rbdip.bookstore.order;

import org.springframework.stereotype.Component;

@Component
public class OrderValidator {

    public static final String REGULAR_CUSTOMER_TYPE = "regular";

    public void validate(CreateOrderRequest request) {
        if (request.customerFullName() == null || request.customerFullName().isBlank()) {
            throw new IllegalArgumentException("customerFullName is required");
        }
        if (request.customerAddress() == null || request.customerAddress().isBlank()) {
            throw new IllegalArgumentException("customerAddress is required");
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("order must contain at least one item");
        }
    }

    public int normalizeQuantity(Integer quantity) {
        int normalizedQuantity = quantity == null ? 1 : quantity;
        if (normalizedQuantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        return normalizedQuantity;
    }
}

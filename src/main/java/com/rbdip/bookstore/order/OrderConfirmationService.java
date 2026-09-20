package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class OrderConfirmationService {

    private static final String CONFIRMATION_TEMPLATE = "[email] Dear %s, your order #%d for %s has been placed.%n";

    public void send(String customerName, Long orderId, BigDecimal total) {
        System.out.printf(CONFIRMATION_TEMPLATE, customerName, orderId, total);
    }
}

package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderValidator orderValidator;
    private final OrderItemResolver orderItemResolver;
    private final PricingCalculator pricingCalculator;
    private final OrderPersistenceService orderPersistenceService;
    private final OrderConfirmationService orderConfirmationService;

    public OrderService(
            OrderValidator orderValidator,
            OrderItemResolver orderItemResolver,
            PricingCalculator pricingCalculator,
            OrderPersistenceService orderPersistenceService,
            OrderConfirmationService orderConfirmationService) {
        this.orderValidator = orderValidator;
        this.orderItemResolver = orderItemResolver;
        this.pricingCalculator = pricingCalculator;
        this.orderPersistenceService = orderPersistenceService;
        this.orderConfirmationService = orderConfirmationService;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        orderValidator.validate(request);
        List<ResolvedOrderItem> items = orderItemResolver.resolve(request.items());
        BigDecimal total = pricingCalculator.calculateOrderTotal(
                items.stream().map(ResolvedOrderItem::toPricingLineItem).toList(),
                request.customerType() == null ? OrderValidator.REGULAR_CUSTOMER_TYPE : request.customerType(),
                request.couponCode());
        Order order = orderPersistenceService.persist(request, items);
        orderConfirmationService.send(request.customerFullName(), order.getId(), total);
        return order;
    }
}

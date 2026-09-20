package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.product.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderValidator orderValidator;

    @Mock
    private OrderItemResolver orderItemResolver;

    @Mock
    private PricingCalculator pricingCalculator;

    @Mock
    private OrderPersistenceService orderPersistenceService;

    @Mock
    private OrderConfirmationService orderConfirmationService;

    @Test
    void usesRegularCustomerTypeByDefaultAndConfirmsPersistedOrder() {
        CreateOrderRequest request = request(null);
        ResolvedOrderItem item = new ResolvedOrderItem(
                new Product("Refactoring", new BigDecimal("12.50"), null), 2);
        Order order = mock(Order.class);
        when(orderItemResolver.resolve(request.items())).thenReturn(List.of(item));
        when(pricingCalculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal("12.50"), 2)), "regular", "SAVE10"))
                .thenReturn(new BigDecimal("15.00"));
        when(orderPersistenceService.persist(request, List.of(item))).thenReturn(order);
        when(order.getId()).thenReturn(91L);

        new OrderService(
                orderValidator,
                orderItemResolver,
                pricingCalculator,
                orderPersistenceService,
                orderConfirmationService)
                .createOrder(request);

        verify(orderValidator).validate(request);
        verify(orderPersistenceService).persist(request, List.of(item));
        verify(orderConfirmationService).send("Ada Lovelace", 91L, new BigDecimal("15.00"));
    }

    @Test
    void stopsBeforeResolvingItemsWhenValidationFails() {
        CreateOrderRequest request = request("vip");
        doThrow(new IllegalArgumentException("order must contain at least one item"))
                .when(orderValidator)
                .validate(request);

        OrderService service = new OrderService(
                orderValidator,
                orderItemResolver,
                pricingCalculator,
                orderPersistenceService,
                orderConfirmationService);

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("order must contain at least one item");
        verifyNoInteractions(orderItemResolver, pricingCalculator, orderPersistenceService, orderConfirmationService);
    }

    private CreateOrderRequest request(String customerType) {
        return new CreateOrderRequest(
                "Ada Lovelace",
                "London",
                null,
                customerType,
                "SAVE10",
                List.of(new CreateOrderRequest.Item(1L, 2)));
    }
}

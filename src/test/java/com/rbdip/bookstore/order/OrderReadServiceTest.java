package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderReadServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    void groupsBulkLoadedItemsByOrderId() {
        Order firstOrder = order(1L, "Ada Lovelace");
        Order secondOrder = order(2L, "Grace Hopper");
        OrderItem firstItem = item(1L, "Refactoring", 1);
        OrderItem secondItem = item(2L, "Clean Code", 2);
        when(orderRepository.findAllWithCustomer()).thenReturn(List.of(firstOrder, secondOrder));
        when(orderItemRepository.findAllByOrderIdInWithProduct(List.of(1L, 2L)))
                .thenReturn(List.of(firstItem, secondItem));

        List<Map<String, Object>> orders = new OrderReadService(orderRepository, orderItemRepository).listOrders();

        assertThat(orders)
                .extracting(order -> order.get("items"))
                .containsExactly(
                        List.of(Map.of("productName", "Refactoring", "quantity", 1)),
                        List.of(Map.of("productName", "Clean Code", "quantity", 2)));
        verify(orderItemRepository).findAllByOrderIdInWithProduct(List.of(1L, 2L));
    }

    @Test
    void skipsItemQueryWhenThereAreNoOrders() {
        when(orderRepository.findAllWithCustomer()).thenReturn(List.of());

        List<Map<String, Object>> orders = new OrderReadService(orderRepository, orderItemRepository).listOrders();

        assertThat(orders).isEmpty();
        verifyNoInteractions(orderItemRepository);
    }

    private Order order(Long id, String customerFullName) {
        Order order = mock(Order.class);
        when(order.getId()).thenReturn(id);
        when(order.getCustomerFullName()).thenReturn(customerFullName);
        when(order.getStatus()).thenReturn("new");
        return order;
    }

    private OrderItem item(Long orderId, String productName, Integer quantity) {
        OrderItem item = mock(OrderItem.class);
        when(item.getOrderId()).thenReturn(orderId);
        when(item.getProductName()).thenReturn(productName);
        when(item.getQuantity()).thenReturn(quantity);
        return item;
    }
}

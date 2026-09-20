package com.rbdip.bookstore.order;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderReadService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderReadService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrders() {
        List<Order> orders = orderRepository.findAllWithCustomer();
        if (orders.isEmpty()) {
            return List.of();
        }

        Map<Long, List<OrderItem>> itemsByOrderId = orderItemRepository
                .findAllByOrderIdInWithProduct(orders.stream().map(Order::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));
        return orders.stream().map(order -> toResponse(order, itemsByOrderId)).toList();
    }

    private Map<String, Object> toResponse(Order order, Map<Long, List<OrderItem>> itemsByOrderId) {
        List<Map<String, Object>> items = itemsByOrderId.getOrDefault(order.getId(), List.of()).stream()
                .map(item -> Map.<String, Object>of(
                        "productName", item.getProductName(), "quantity", item.getQuantity()))
                .toList();
        return Map.of(
                "id", order.getId(),
                "customerFullName", order.getCustomerFullName(),
                "status", order.getStatus(),
                "items", items);
    }
}

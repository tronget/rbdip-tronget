package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderPersistenceService {

    private static final String NEW_ORDER_STATUS = "new";

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistenceService(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order persist(CreateOrderRequest request, List<ResolvedOrderItem> resolvedItems) {
        Order order = orderRepository.save(new Order(
                request.customerFullName(), request.customerAddress(), request.customerPhone(), NEW_ORDER_STATUS));
        for (ResolvedOrderItem resolvedItem : resolvedItems) {
            Product product = resolvedItem.product();
            orderItemRepository.save(
                    new OrderItem(order.getId(), product.getName(), product.getPrice(), resolvedItem.quantity()));
        }
        return order;
    }
}

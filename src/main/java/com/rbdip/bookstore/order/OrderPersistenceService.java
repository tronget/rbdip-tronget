package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerName;
import com.rbdip.bookstore.customer.CustomerRepository;
import com.rbdip.bookstore.product.Product;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderPersistenceService {

    private static final String NEW_ORDER_STATUS = "new";

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderPersistenceService(
            CustomerRepository customerRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order persist(CreateOrderRequest request, List<ResolvedOrderItem> resolvedItems) {
        CustomerName customerName = CustomerName.from(request.customerFullName());
        Customer customer = customerRepository
                .findByFirstNameAndLastNameAndAddressAndPhone(
                        customerName.firstName(),
                        customerName.lastName(),
                        request.customerAddress(),
                        request.customerPhone())
                .orElseGet(() -> customerRepository.save(
                        new Customer(request.customerFullName(), request.customerAddress(), request.customerPhone())));
        Order order = orderRepository.save(new Order(customer, NEW_ORDER_STATUS));
        for (ResolvedOrderItem resolvedItem : resolvedItems) {
            Product product = resolvedItem.product();
            orderItemRepository.save(
                    new OrderItem(order.getId(), product, resolvedItem.quantity()));
        }
        return order;
    }
}

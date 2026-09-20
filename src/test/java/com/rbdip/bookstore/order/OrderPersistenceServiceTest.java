package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerRepository;
import com.rbdip.bookstore.product.Product;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPersistenceServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    void reusesMatchingCustomerAndPersistsEveryResolvedItem() {
        Customer customer = mock(Customer.class);
        Order savedOrder = mock(Order.class);
        Product product = new Product("Domain-Driven Design", new BigDecimal("50.00"), null);
        CreateOrderRequest request = request();
        when(customerRepository.findByFirstNameAndLastNameAndAddressAndPhone("Ada", "Lovelace", "London", null))
                .thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(savedOrder.getId()).thenReturn(12L);

        new OrderPersistenceService(customerRepository, orderRepository, orderItemRepository)
                .persist(request, List.of(new ResolvedOrderItem(product, 2)));

        verify(customerRepository, never()).save(any(Customer.class));
        ArgumentCaptor<OrderItem> itemCaptor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(itemCaptor.capture());
        assertThat(itemCaptor.getValue().getOrderId()).isEqualTo(12L);
        assertThat(itemCaptor.getValue().getProductName()).isEqualTo("Domain-Driven Design");
        assertThat(itemCaptor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void createsCustomerFromRequestWhenThereIsNoMatchingContact() {
        Order savedOrder = mock(Order.class);
        CreateOrderRequest request = request();
        when(customerRepository.findByFirstNameAndLastNameAndAddressAndPhone("Ada", "Lovelace", "London", null))
                .thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        new OrderPersistenceService(customerRepository, orderRepository, orderItemRepository)
                .persist(request, List.of());

        ArgumentCaptor<Customer> customerCaptor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(customerCaptor.capture());
        assertThat(customerCaptor.getValue().getFullName()).isEqualTo("Ada Lovelace");
        assertThat(customerCaptor.getValue().getAddress()).isEqualTo("London");
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest("Ada Lovelace", "London", null, "regular", null, List.of());
    }
}

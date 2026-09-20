package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderItemResolverTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderValidator orderValidator;

    @Test
    void resolvesProductsAndNormalizesTheirQuantities() {
        Product product = new Product("Clean Code", new BigDecimal("30.00"), null);
        CreateOrderRequest.Item requestedItem = new CreateOrderRequest.Item(7L, null);
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(orderValidator.normalizeQuantity(null)).thenReturn(1);

        List<ResolvedOrderItem> resolvedItems = new OrderItemResolver(productRepository, orderValidator)
                .resolve(List.of(requestedItem));

        assertThat(resolvedItems).containsExactly(new ResolvedOrderItem(product, 1));
        verify(orderValidator).normalizeQuantity(null);
    }

    @Test
    void rejectsAnItemWhoseProductDoesNotExist() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new OrderItemResolver(productRepository, orderValidator)
                        .resolve(List.of(new CreateOrderRequest.Item(404L, 1))))
                .withMessage("product 404 not found");
    }
}

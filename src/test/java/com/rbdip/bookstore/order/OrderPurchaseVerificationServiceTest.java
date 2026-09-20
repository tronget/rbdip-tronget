package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderPurchaseVerificationServiceTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    void delegatesPurchaseLookupToOrderItemsWithoutExposingOrderToReview() {
        when(orderItemRepository.existsForProductId(42L)).thenReturn(true);

        boolean purchased = new OrderPurchaseVerificationService(orderItemRepository).hasPurchasesForProduct(42L);

        assertThat(purchased).isTrue();
        verify(orderItemRepository).existsForProductId(42L);
    }
}

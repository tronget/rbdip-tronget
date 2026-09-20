package com.rbdip.bookstore.order;

import com.rbdip.bookstore.purchase.PurchaseVerificationService;
import org.springframework.stereotype.Service;

@Service
public class OrderPurchaseVerificationService implements PurchaseVerificationService {

    private final OrderItemRepository orderItemRepository;

    public OrderPurchaseVerificationService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    public boolean hasPurchasesForProduct(Long productId) {
        return orderItemRepository.existsForProductId(productId);
    }
}

package com.rbdip.bookstore.order;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OrderItemResolver {

    private final ProductRepository productRepository;
    private final OrderValidator orderValidator;

    public OrderItemResolver(ProductRepository productRepository, OrderValidator orderValidator) {
        this.productRepository = productRepository;
        this.orderValidator = orderValidator;
    }

    public List<ResolvedOrderItem> resolve(List<CreateOrderRequest.Item> items) {
        List<ResolvedOrderItem> resolvedItems = new ArrayList<>();
        for (CreateOrderRequest.Item item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new IllegalArgumentException("product " + item.productId() + " not found"));
            resolvedItems.add(new ResolvedOrderItem(product, orderValidator.normalizeQuantity(item.quantity())));
        }
        return resolvedItems;
    }
}

package com.rbdip.bookstore.purchase;

/**
 * Read-only boundary through which modules can ask whether a product was purchased.
 */
public interface PurchaseVerificationService {

    boolean hasPurchasesForProduct(Long productId);
}

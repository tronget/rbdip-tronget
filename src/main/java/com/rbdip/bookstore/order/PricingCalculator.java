package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Модуль расчёта цены заказа. Намеренно почти не покрыт тестами и
 * содержит magic numbers / нечитаемые ветвления скидок - цель для
 * характеризационных тестов (ЛР2) и mutation-testing гейта PIT (ЛР5).
 */
@Component
public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_DISCOUNT_FACTOR = new BigDecimal("0.95");
    private static final BigDecimal VIP_DISCOUNT_FACTOR = new BigDecimal("0.90");
    private static final BigDecimal WHOLESALE_DISCOUNT_FACTOR = new BigDecimal("0.85");
    private static final BigDecimal SAVE_10_DISCOUNT = BigDecimal.TEN;
    private static final BigDecimal SAVE_20_PERCENT_FACTOR = new BigDecimal("0.80");
    private static final BigDecimal LARGE_ORDER_THRESHOLD = new BigDecimal("1000");
    private static final BigDecimal LARGE_ORDER_DISCOUNT_FACTOR = new BigDecimal("0.98");
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final String VIP_CUSTOMER_TYPE = "vip";
    private static final String WHOLESALE_CUSTOMER_TYPE = "wholesale";
    private static final String SAVE_10_COUPON = "SAVE10";
    private static final String SAVE_20_PERCENT_COUPON = "SAVE20PERCENT";
    private static final Map<String, BigDecimal> CUSTOMER_DISCOUNT_FACTORS = Map.of(
            VIP_CUSTOMER_TYPE, VIP_DISCOUNT_FACTOR,
            WHOLESALE_CUSTOMER_TYPE, WHOLESALE_DISCOUNT_FACTOR);

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            total = total.add(calculateLineTotal(item));
        }

        total = applyCustomerDiscount(total, customerType);
        total = applyCoupon(total, couponCode);
        total = total.max(BigDecimal.ZERO);
        return normalizeTotal(applyLargeOrderDiscount(total));
    }

    private BigDecimal calculateLineTotal(LineItem item) {
        BigDecimal lineTotal = item.price().multiply(BigDecimal.valueOf(item.quantity()));
        return item.quantity() > BULK_QUANTITY_THRESHOLD
                ? lineTotal.multiply(BULK_DISCOUNT_FACTOR)
                : lineTotal;
    }

    private BigDecimal applyCustomerDiscount(BigDecimal total, String customerType) {
        BigDecimal discountFactor = customerType == null
                ? BigDecimal.ONE
                : CUSTOMER_DISCOUNT_FACTORS.getOrDefault(customerType, BigDecimal.ONE);
        return total.multiply(discountFactor);
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if (SAVE_10_COUPON.equals(couponCode)) {
            return total.subtract(SAVE_10_DISCOUNT);
        }
        if (SAVE_20_PERCENT_COUPON.equals(couponCode)) {
            return total.multiply(SAVE_20_PERCENT_FACTOR);
        }
        return total;
    }

    private BigDecimal applyLargeOrderDiscount(BigDecimal total) {
        return total.compareTo(LARGE_ORDER_THRESHOLD) > 0
                ? total.multiply(LARGE_ORDER_DISCOUNT_FACTOR)
                : total;
    }

    private BigDecimal normalizeTotal(BigDecimal total) {
        return total.setScale(MONEY_SCALE, MONEY_ROUNDING_MODE);
    }
}

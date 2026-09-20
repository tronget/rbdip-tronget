package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void returnsZeroForAnEmptyOrder() {
        assertTotal("0.00", List.of(), "regular", null);
    }

    @Test
    void appliesBulkDiscountOnlyAboveTheQuantityThreshold() {
        assertTotal("100.00", List.of(line("10.00", 10)), "regular", null);
        assertTotal("104.50", List.of(line("10.00", 11)), "regular", null);
    }

    @Test
    void appliesKnownCustomerDiscounts() {
        List<PricingCalculator.LineItem> items = List.of(line("100.00", 1));

        assertTotal("90.00", items, "vip", null);
        assertTotal("85.00", items, "wholesale", null);
    }

    @Test
    void ignoresUnknownNullAndCaseChangedCustomerTypes() {
        List<PricingCalculator.LineItem> items = List.of(line("100.00", 1));

        assertTotal("100.00", items, null, null);
        assertTotal("100.00", items, "regular", null);
        assertTotal("100.00", items, "partner", null);
        assertTotal("100.00", items, "VIP", null);
    }

    @Test
    void appliesKnownCoupons() {
        List<PricingCalculator.LineItem> items = List.of(line("100.00", 1));

        assertTotal("90.00", items, "regular", "SAVE10");
        assertTotal("80.00", items, "regular", "SAVE20PERCENT");
    }

    @Test
    void ignoresUnknownNullAndCaseChangedCoupons() {
        List<PricingCalculator.LineItem> items = List.of(line("100.00", 1));

        assertTotal("100.00", items, "regular", null);
        assertTotal("100.00", items, "regular", "save10");
        assertTotal("100.00", items, "regular", "SAVE15");
    }

    @Test
    void clampsCouponAndNegativeLineTotalsAtZero() {
        assertTotal("0.00", List.of(line("5.00", 1)), "regular", "SAVE10");
        assertTotal("0.00", List.of(line("-10.00", 1)), "regular", null);
    }

    @Test
    void appliesLargeOrderDiscountOnlyAboveOneThousand() {
        assertTotal("1000.00", List.of(line("1000.00", 1)), "regular", null);
        assertTotal("980.01", List.of(line("1000.01", 1)), "regular", null);
    }

    @Test
    void appliesDiscountsInTheirEstablishedOrder() {
        assertTotal("1754.20", List.of(line("2000.00", 1)), "vip", "SAVE10");
    }

    @Test
    void roundsMoneyHalfUpToTwoDecimalPlaces() {
        assertTotal("0.01", List.of(line("0.005", 1)), "regular", null);
    }

    private PricingCalculator.LineItem line(String price, int quantity) {
        return new PricingCalculator.LineItem(new BigDecimal(price), quantity);
    }

    private void assertTotal(
            String expected,
            List<PricingCalculator.LineItem> items,
            String customerType,
            String couponCode) {
        assertThat(calculator.calculateOrderTotal(items, customerType, couponCode))
                .isEqualByComparingTo(expected);
    }
}

package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class OrderValidatorTest {

    private final OrderValidator validator = new OrderValidator();

    @Test
    void rejectsMissingRequiredOrderFields() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validate(request(null, "Address", List.of(item()))))
                .withMessage("customerFullName is required");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validate(request("Customer", " ", List.of(item()))))
                .withMessage("customerAddress is required");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.validate(request("Customer", "Address", List.of())))
                .withMessage("order must contain at least one item");
    }

    @Test
    void defaultsMissingQuantityToOneAndRejectsNonPositiveValues() {
        assertThat(validator.normalizeQuantity(null)).isOne();
        assertThat(validator.normalizeQuantity(3)).isEqualTo(3);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.normalizeQuantity(0))
                .withMessage("quantity must be positive");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> validator.normalizeQuantity(-1))
                .withMessage("quantity must be positive");
    }

    private CreateOrderRequest request(String fullName, String address, List<CreateOrderRequest.Item> items) {
        return new CreateOrderRequest(fullName, address, null, "regular", null, items);
    }

    private CreateOrderRequest.Item item() {
        return new CreateOrderRequest.Item(1L, 1);
    }
}

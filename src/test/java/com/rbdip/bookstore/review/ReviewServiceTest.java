package com.rbdip.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.purchase.PurchaseVerificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private PurchaseVerificationService purchaseVerificationService;

    @Test
    void checksPurchasesThroughThePortWithoutChangingReviewCreation() {
        Long productId = 42L;
        when(purchaseVerificationService.hasPurchasesForProduct(productId)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Review review = new ReviewService(reviewRepository, purchaseVerificationService)
                .addReview(productId, null, 5, "Excellent");

        assertThat(review.getProductId()).isEqualTo(productId);
        assertThat(review.getAuthorName()).isEqualTo("anonymous");
        verify(purchaseVerificationService).hasPurchasesForProduct(productId);
    }
}

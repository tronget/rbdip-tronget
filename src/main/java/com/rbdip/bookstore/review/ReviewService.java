package com.rbdip.bookstore.review;

import com.rbdip.bookstore.purchase.PurchaseVerificationService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PurchaseVerificationService purchaseVerificationService;

    public ReviewService(ReviewRepository reviewRepository, PurchaseVerificationService purchaseVerificationService) {
        this.reviewRepository = reviewRepository;
        this.purchaseVerificationService = purchaseVerificationService;
    }

    public Review addReview(Long productId, String authorName, Integer rating, String comment) {
        // The current API deliberately does not expose or enforce this result yet.
        purchaseVerificationService.hasPurchasesForProduct(productId);
        Review review = new Review(productId, authorName == null ? "anonymous" : authorName, rating, comment);
        return reviewRepository.save(review);
    }

    public List<Review> listReviews(Long productId) {
        return reviewRepository.findByProductId(productId);
    }
}

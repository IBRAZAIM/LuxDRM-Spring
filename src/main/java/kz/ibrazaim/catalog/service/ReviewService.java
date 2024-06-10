package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Review;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;

    @Transactional
    public void create(Product product, User user, int estimation, String text) {
        Review review = new Review();

        review.setUser(user);
        review.setProduct(product);
        review.setEstimation(estimation);
        review.setText(text);
        review.setDate(LocalDateTime.now());
        review.setStatus(true);
        reviewRepository.save(review);
    }


    public List<Review> getCommentsForProduct(Product product) {
        System.out.println(reviewRepository.findByProduct(product));
        return reviewRepository.findByProduct(product);
    }
}

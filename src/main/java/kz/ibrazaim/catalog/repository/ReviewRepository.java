package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByProduct(Product product);
}

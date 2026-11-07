package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Review;
import kz.ibrazaim.catalog.model.ReviewStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Override
    @EntityGraph(attributePaths = {"user", "product"})
    List<Review> findAll();

    @EntityGraph(attributePaths = {"user", "product"})
    List<Review> findAllByOrderByDateDescIdDesc();

    @EntityGraph(attributePaths = {"user"})
    List<Review> findByProductAndStatus(Product product, ReviewStatus status);

    void deleteAllByProductId(long productId);

    @Query("SELECT AVG(r.estimation) FROM Review r WHERE r.product = :product AND r.status = :status")
    Double findAverageRatingByProductAndStatus(@Param("product") Product product, @Param("status") ReviewStatus status);
}

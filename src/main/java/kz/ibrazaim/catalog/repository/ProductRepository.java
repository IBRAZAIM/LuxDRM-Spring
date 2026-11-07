package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = {"category"})
    List<Product> findByDeletedFalse();

    @EntityGraph(attributePaths = {"category"})
    List<Product> findTop8ByCategoryIdAndIdNotAndDeletedFalseOrderByIdDesc(Long categoryId, Long excludeId);

    @EntityGraph(attributePaths = {"category"})
    List<Product> findTop8ByIdNotAndDeletedFalseOrderByIdDesc(Long excludeId);

    @Query("SELECT p.id FROM Product p WHERE p.deleted = false ORDER BY p.id")
    List<Long> findAllActiveIds();

    @EntityGraph(attributePaths = {"category"})
    Optional<Product> findByIdAndDeletedFalse(Long id);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByPriceBetween(@Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.deleted = false AND p.category.id = :categoryId AND p.price BETWEEN :minPrice AND :maxPrice")
    List<Product> findByCategoryIdAndPriceBetween(@Param("categoryId") Long categoryId,
                                                  @Param("minPrice") BigDecimal minPrice,
                                                  @Param("maxPrice") BigDecimal maxPrice);

    @EntityGraph(attributePaths = {"category"})
    @Query("SELECT p FROM Product p WHERE p.deleted = false " +
           "AND (:search IS NULL OR :search = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND p.price >= :minPrice AND p.price <= :maxPrice")
    Page<Product> searchProducts(
            @Param("search") String search,
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable
    );
}

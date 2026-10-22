package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.util.List;

@Component
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndPriceBetween(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice);
    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
}


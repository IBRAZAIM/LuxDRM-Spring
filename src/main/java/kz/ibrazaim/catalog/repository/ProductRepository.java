package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategoryIdAndPriceBetween(Long categoryId, int minPrice, int maxPrice);
    List<Product> findByPriceBetween(int minPrice, int maxPrice);
}


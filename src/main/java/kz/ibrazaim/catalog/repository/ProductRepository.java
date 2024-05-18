package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByPriceBetween(Double minPrice, Double maxPrice);
    List<Product> findByPriceLessThanEqual(Double maxPrice);
    List<Product> findByPriceGreaterThanEqual(Double minPrice);
}


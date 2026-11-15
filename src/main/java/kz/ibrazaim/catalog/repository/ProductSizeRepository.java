package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductSize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    List<ProductSize> findAllByProduct(Product product);

    Optional<ProductSize> findFirstByProductAndSizeIgnoreCase(Product product, String size);

    void deleteAllByProduct(Product product);
}

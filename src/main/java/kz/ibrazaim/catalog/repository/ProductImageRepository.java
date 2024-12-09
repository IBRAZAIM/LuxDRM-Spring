package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage,Long> {
    ProductImage findByProduct(Product product);

    List<ProductImage> findAllByProduct(Product product);

    void deleteByProductId(Long productId);
}

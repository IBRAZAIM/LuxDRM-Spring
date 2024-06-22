package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductImage;
import kz.ibrazaim.catalog.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductImageRepository productImageRepository;

    public ProductImage create(Product product, String imageUrl){
        ProductImage productImage = new ProductImage();
        productImage.setProduct(product);
        productImage.setImage(imageUrl);
        productImageRepository.save(productImage);
        return productImage;
    }

    public ProductImage findByProduct(Product product){
        return productImageRepository.findByProduct(product);
    }
}

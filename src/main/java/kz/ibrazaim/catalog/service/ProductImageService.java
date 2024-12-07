package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductImage;
import kz.ibrazaim.catalog.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductImageService {
    private final ProductImageRepository productImageRepository;

    public void create(Product product, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new IllegalArgumentException("Image URLs cannot be null or empty");
        }
        // Создаём новый объект ProductImage и добавляем в коллекцию images
        for (String imageUrl : imageUrls){
            ProductImage productImage = new ProductImage();
            productImage.setProduct(product);
            productImage.setImage(imageUrl);  // Добавляем изображение
            // Сохраняем в базу
            productImageRepository.save(productImage);
        }
    }

    public ProductImage findByProduct(Product product){
        return productImageRepository.findByProduct(product);
    }
}

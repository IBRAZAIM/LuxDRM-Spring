package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductSize;
import kz.ibrazaim.catalog.repository.ProductSizeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductSizeService {
    @Autowired
    ProductSizeRepository productSizeRepository;

    public void saveSizes(Product product, List<String> sizes) {
        for (String size : sizes) {
            ProductSize productSize = new ProductSize();
            productSize.setProduct(product);
            productSize.setSize(size);
            productSizeRepository.save(productSize);
        }
    }
    public List<ProductSize> findByProduct(Product product) {
        return productSizeRepository.findAllByProduct(product);
    }
}


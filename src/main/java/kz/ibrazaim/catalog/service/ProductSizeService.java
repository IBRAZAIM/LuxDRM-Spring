package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductSize;
import kz.ibrazaim.catalog.repository.ProductSizeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductSizeService {
    private final ProductSizeRepository productSizeRepository;

    @Transactional
    public void saveSizes(Product product, List<String> sizes) {
        saveSizes(product, sizes, Map.of());
    }

    @Transactional
    public void replaceSizes(Product product, List<String> sizes) {
        List<ProductSize> existing = productSizeRepository.findAllByProduct(product);
        Map<String, Integer> existingStockMap = new LinkedHashMap<>();
        for (ProductSize ps : existing) {
            if (ps.getSize() != null) {
                existingStockMap.put(ps.getSize().trim().toLowerCase(Locale.ROOT), ps.getStock());
            }
        }
        productSizeRepository.deleteAllByProduct(product);
        productSizeRepository.flush();
        saveSizes(product, sizes, existingStockMap);
    }

    private void saveSizes(Product product, List<String> sizes, Map<String, Integer> existingStockMap) {
        if (sizes == null || sizes.isEmpty()) {
            return;
        }
        Set<String> uniqueSizes = new LinkedHashSet<>();
        for (String size : sizes) {
            if (size != null && !size.trim().isEmpty()) {
                uniqueSizes.add(size.trim());
            }
        }
        int defaultStock = Math.max(1, product.getStock() > 0 ? product.getStock() : 15);
        for (String cleanSize : uniqueSizes) {
            ProductSize productSize = new ProductSize();
            productSize.setProduct(product);
            productSize.setSize(cleanSize);
            Integer preservedStock = existingStockMap.get(cleanSize.toLowerCase(Locale.ROOT));
            productSize.setStock(preservedStock != null ? preservedStock : defaultStock);
            productSizeRepository.save(productSize);
        }
    }

    public List<ProductSize> findByProduct(Product product) {
        return productSizeRepository.findAllByProduct(product);
    }
}

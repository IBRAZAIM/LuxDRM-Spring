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
    public int saveSizesWithStocks(Product product, List<String> sizes, List<Integer> sizeStocks) {
        if (sizes == null || sizes.isEmpty()) {
            return 0;
        }
        Map<String, String> displayByKey = new LinkedHashMap<>();
        Map<String, Integer> stockByKey = new LinkedHashMap<>();

        for (int i = 0; i < sizes.size(); i++) {
            String rawSize = sizes.get(i);
            if (rawSize == null || rawSize.trim().isEmpty()) {
                continue;
            }
            String cleanSize = rawSize.trim();
            String key = cleanSize.toLowerCase(Locale.ROOT);
            displayByKey.putIfAbsent(key, cleanSize);

            int rowStock = (sizeStocks != null && i < sizeStocks.size() && sizeStocks.get(i) != null)
                    ? Math.max(0, sizeStocks.get(i))
                    : 0;
            stockByKey.put(key, rowStock);
        }

        int totalStock = 0;
        for (Map.Entry<String, String> entry : displayByKey.entrySet()) {
            int entryStock = stockByKey.getOrDefault(entry.getKey(), 0);
            ProductSize productSize = new ProductSize();
            productSize.setProduct(product);
            productSize.setSize(entry.getValue());
            productSize.setStock(entryStock);
            ProductSize saved = productSizeRepository.save(productSize);
            if (product.getSizes() != null) {
                product.getSizes().add(saved);
            }
            totalStock += entryStock;
        }
        return totalStock;
    }

    @Transactional
    public int replaceSizesWithStocks(Product product, List<String> sizes, List<Integer> sizeStocks) {
        if (product.getSizes() != null) {
            product.getSizes().clear();
        }
        productSizeRepository.deleteAllByProduct(product);
        productSizeRepository.flush();
        return saveSizesWithStocks(product, sizes, sizeStocks);
    }

    @Transactional
    public void saveSizes(Product product, List<String> sizes) {
        saveSizes(product, sizes, Map.of(), false);
    }

    @Transactional
    public void replaceSizes(Product product, List<String> sizes) {
        replaceSizes(product, sizes, false);
    }

    @Transactional
    public void replaceSizes(Product product, List<String> sizes, boolean stockChanged) {
        List<ProductSize> existing = productSizeRepository.findAllByProduct(product);
        Map<String, Integer> existingStockMap = new LinkedHashMap<>();
        for (ProductSize ps : existing) {
            if (ps.getSize() != null) {
                existingStockMap.put(ps.getSize().trim().toLowerCase(Locale.ROOT), ps.getStock());
            }
        }
        if (product.getSizes() != null) {
            product.getSizes().clear();
        }
        productSizeRepository.deleteAllByProduct(product);
        productSizeRepository.flush();
        saveSizes(product, sizes, existingStockMap, stockChanged);
    }

    private void saveSizes(Product product, List<String> sizes, Map<String, Integer> existingStockMap, boolean stockChanged) {
        if (sizes == null || sizes.isEmpty()) {
            return;
        }
        Set<String> uniqueSizes = new LinkedHashSet<>();
        for (String size : sizes) {
            if (size != null && !size.trim().isEmpty()) {
                uniqueSizes.add(size.trim());
            }
        }
        int productStock = Math.max(0, product.getStock());
        for (String cleanSize : uniqueSizes) {
            ProductSize productSize = new ProductSize();
            productSize.setProduct(product);
            productSize.setSize(cleanSize);
            Integer preservedStock = existingStockMap.get(cleanSize.toLowerCase(Locale.ROOT));
            int sizeStock;
            if (productStock == 0) {
                sizeStock = 0;
            } else if (stockChanged || preservedStock == null || preservedStock <= 0) {
                sizeStock = productStock;
            } else {
                sizeStock = preservedStock;
            }
            productSize.setStock(sizeStock);
            ProductSize saved = productSizeRepository.save(productSize);
            if (product.getSizes() != null) {
                product.getSizes().add(saved);
            }
        }
    }

    public List<ProductSize> findByProduct(Product product) {
        return productSizeRepository.findAllByProduct(product);
    }
}

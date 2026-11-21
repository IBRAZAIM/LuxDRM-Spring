package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.exception.EntityNotFoundException;
import kz.ibrazaim.catalog.model.*;
import kz.ibrazaim.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService implements AbstractService<Product> {
    public record DeleteResult(String productName, boolean hardDeleted) {}

    private final ProductRepository productRepository;
    private final ValueRepository valueRepository;
    private final OptionRepository optionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageService productImageService;
    private final ProductSizeService productSizeService;
    private final ProductImageRepository productImageRepository;
    private final ProductSizeRepository productSizeRepository;
    private final ReviewRepository reviewRepository;
    private final OrderProductRepository orderProductRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistRepository wishlistRepository;

    @Override
    public void create(Product product) {
        productRepository.save(product);
    }

    @Override
    public void deleteById(long id) {
        deleteProductById(id);
    }

    @Transactional
    @Override
    public void deleteProductById(long productId) {
        deleteProductWithResult(productId);
    }

    @Transactional
    public DeleteResult deleteProductWithResult(long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Товар с указанным id=" + productId + " не найден"));
        String productName = (product.getName() != null && !product.getName().isBlank())
                ? product.getName().trim()
                : ("#" + productId);

        cartItemRepository.deleteAllByProductId(productId);
        wishlistRepository.deleteAllByProductId(productId);

        if (orderProductRepository.existsByProductId(productId)) {
            product.setDeleted(true);
            productRepository.save(product);
            return new DeleteResult(productName, false);
        }

        List<String> imagePaths = productImageRepository.findAllByProduct(product).stream()
                .map(ProductImage::getImage)
                .filter(Objects::nonNull)
                .toList();

        reviewRepository.deleteAllByProductId(productId);
        valueRepository.deleteAllByProductId(productId);

        if (product.getImages() != null) {
            product.getImages().clear();
        }
        if (product.getSizes() != null) {
            product.getSizes().clear();
        }
        productImageRepository.deleteAllByProduct(product);
        productSizeRepository.deleteAllByProduct(product);
        productRepository.flush();

        productRepository.delete(product);
        productRepository.flush();

        if (!imagePaths.isEmpty()) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        productImageService.deleteLocalFiles(imagePaths);
                    }
                });
            } else {
                productImageService.deleteLocalFiles(imagePaths);
            }
        }
        return new DeleteResult(productName, true);
    }

    @Transactional
    public void create(
            Product product,
            List<String> values,
            List<Long> optionsIds,
            Long categoryId,
            List<MultipartFile> imageFiles,
            List<String> sizes
    ) {
        create(product, values, optionsIds, categoryId, imageFiles, sizes, List.of());
    }

    @Transactional
    public void create(
            Product product,
            List<String> values,
            List<Long> optionsIds,
            Long categoryId,
            List<MultipartFile> imageFiles,
            List<String> sizes,
            List<Integer> sizeStocks
    ) {
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Название товара не может быть пустым");
        }
        if (product.getSku() == null || product.getSku().trim().isEmpty()) {
            throw new IllegalArgumentException("Артикул товара обязателен");
        }
        product.setName(product.getName().trim());
        product.setSku(product.getSku().trim());

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        product.setCategory(category);

        if (product.getRating() <= 0) {
            product.setRating(5.0);
        }
        if (product.getAudience() == null) {
            product.setAudience(ProductAudience.UNISEX);
        }
        if (product.getDiscount() == null || product.getDiscount().compareTo(BigDecimal.ZERO) < 0) {
            product.setDiscount(BigDecimal.ZERO);
        } else if (product.getDiscount().compareTo(new BigDecimal("99")) > 0) {
            product.setDiscount(new BigDecimal("99"));
        }
        product.setStock(0);

        Product savedProduct = productRepository.save(product);

        if (optionsIds != null && !optionsIds.isEmpty()) {
            createValues(savedProduct, values != null ? values : List.of(), optionsIds);
        }

        if (imageFiles != null && !imageFiles.isEmpty()) {
            productImageService.saveUploadedFiles(savedProduct, imageFiles);
        }

        if (sizes != null && !sizes.isEmpty()) {
            int totalStock = productSizeService.saveSizesWithStocks(savedProduct, sizes, sizeStocks);
            savedProduct.setStock(totalStock);
            productRepository.save(savedProduct);
        }
    }

    private void createValues(Product product, List<String> values, List<Long> optionsIds) {
        Map<Long, Option> optionsById = new HashMap<>();
        for (Option option : optionRepository.findAllById(optionsIds)) {
            optionsById.put(option.getId(), option);
        }
        List<Value> toSave = new ArrayList<>(optionsIds.size());

        for (int i = 0; i < optionsIds.size(); i++) {
            Long optionId = optionsIds.get(i);
            if (optionId == null) {
                continue;
            }
            Option option = optionsById.get(optionId);
            if (option == null) {
                continue;
            }
            String valueText = (values != null && i < values.size() && values.get(i) != null)
                    ? values.get(i).trim()
                    : "";

            Value value = new Value();
            value.setName(valueText);
            value.setProduct(product);
            value.setOption(option);
            toSave.add(value);
        }
        valueRepository.saveAll(toSave);
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findByDeletedFalse();
    }

    public List<Long> findAllActiveIds() {
        return productRepository.findAllActiveIds();
    }

    public List<Product> findSimilarProducts(Product product) {
        if (product == null) {
            return List.of();
        }
        if (product.getCategory() != null) {
            List<Product> sameCategory = productRepository.findTop8ByCategoryIdAndIdNotAndDeletedFalseOrderByIdDesc(
                    product.getCategory().getId(),
                    product.getId()
            );
            if (!sameCategory.isEmpty()) {
                return sameCategory;
            }
        }
        return productRepository.findTop8ByIdNotAndDeletedFalseOrderByIdDesc(product.getId());
    }

    @Override
    public Product findById(long id) {
        return productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Товар с указанным id=" + id + " не найден"));
    }

    @Transactional
    public void update(
            long id,
            String name,
            BigDecimal price,
            BigDecimal discount,
            Integer stock,
            String description,
            List<Long> optionIds,
            List<String> values,
            List<String> sizes,
            List<String> existingImages,
            List<MultipartFile> imageFiles
    ) {
        update(id, name, price, discount, stock, description, null, optionIds, values, sizes, existingImages, imageFiles);
    }

    @Transactional
    public void update(
            long id,
            String name,
            BigDecimal price,
            BigDecimal discount,
            Integer stock,
            String description,
            Long categoryId,
            List<Long> optionIds,
            List<String> values,
            List<String> sizes,
            List<String> existingImages,
            List<MultipartFile> imageFiles
    ) {
        Product existing = findById(id);
        int fallbackStock = Math.max(0, stock != null ? stock : existing.getStock());
        List<Integer> fallbackStocks = sizes != null
                ? Collections.nCopies(sizes.size(), fallbackStock)
                : List.of();
        update(id, name, existing.getDisplaySku(), price, discount, description, categoryId,
                optionIds, values, sizes, fallbackStocks, existingImages, imageFiles);
    }

    @Transactional
    public void update(
            long id,
            String name,
            String sku,
            BigDecimal price,
            BigDecimal discount,
            String description,
            Long categoryId,
            List<Long> optionIds,
            List<String> values,
            List<String> sizes,
            List<Integer> sizeStocks,
            List<String> existingImages,
            List<MultipartFile> imageFiles
    ) {
        update(id, name, sku, null, price, discount, description, categoryId,
                optionIds, values, sizes, sizeStocks, existingImages, imageFiles);
    }

    @Transactional
    public void update(
            long id,
            String name,
            String sku,
            ProductAudience audience,
            BigDecimal price,
            BigDecimal discount,
            String description,
            Long categoryId,
            List<Long> optionIds,
            List<String> values,
            List<String> sizes,
            List<Integer> sizeStocks,
            List<String> existingImages,
            List<MultipartFile> imageFiles
    ) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Название товара не может быть пустым");
        }
        if (sku == null || sku.trim().isEmpty()) {
            throw new IllegalArgumentException("Артикул товара обязателен");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Цена должна быть неотрицательной");
        }

        Product existingProduct = findById(id);

        boolean categoryChanged = false;
        if (categoryId != null && (existingProduct.getCategory() == null || existingProduct.getCategory().getId() != categoryId)) {
            Category newCategory = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Категория с id=" + categoryId + " не найдена"));
            existingProduct.setCategory(newCategory);
            categoryChanged = true;
        }

        existingProduct.setName(name.trim());
        existingProduct.setSku(sku.trim());
        if (audience != null) {
            existingProduct.setAudience(audience);
        } else if (existingProduct.getAudience() == null) {
            existingProduct.setAudience(ProductAudience.UNISEX);
        }
        existingProduct.setPrice(price);
        if (discount != null) {
            if (discount.compareTo(BigDecimal.ZERO) < 0) {
                existingProduct.setDiscount(BigDecimal.ZERO);
            } else if (discount.compareTo(new BigDecimal("99")) > 0) {
                existingProduct.setDiscount(new BigDecimal("99"));
            } else {
                existingProduct.setDiscount(discount);
            }
        } else {
            existingProduct.setDiscount(BigDecimal.ZERO);
        }
        existingProduct.setDescription(description != null ? description.trim() : "");

        int totalStock = productSizeService.replaceSizesWithStocks(
                existingProduct,
                sizes != null ? sizes : List.of(),
                sizeStocks
        );
        existingProduct.setStock(totalStock);

        productRepository.save(existingProduct);

        List<Value> existingValues = valueRepository.findByProduct(existingProduct);
        Map<Long, Value> existingValuesByOptionId = new HashMap<>();
        List<Value> valuesToDelete = new ArrayList<>();
        Long currentCategoryId = existingProduct.getCategory() != null ? existingProduct.getCategory().getId() : null;

        for (Value v : existingValues) {
            Option opt = v.getOption();
            if (opt == null) {
                valuesToDelete.add(v);
                continue;
            }
            if (categoryChanged && currentCategoryId != null
                    && (opt.getCategory() == null || opt.getCategory().getId() != currentCategoryId)) {
                valuesToDelete.add(v);
                continue;
            }
            if (existingValuesByOptionId.containsKey(opt.getId())) {
                valuesToDelete.add(v);
            } else {
                existingValuesByOptionId.put(opt.getId(), v);
            }
        }
        if (!valuesToDelete.isEmpty()) {
            valueRepository.deleteAll(valuesToDelete);
            valueRepository.flush();
        }

        if (optionIds != null && !optionIds.isEmpty()) {
            Map<Long, Option> optionsById = new HashMap<>();
            for (Option option : optionRepository.findAllById(optionIds)) {
                optionsById.put(option.getId(), option);
            }
            List<Value> toSave = new ArrayList<>(optionIds.size());
            for (int i = 0; i < optionIds.size(); i++) {
                Long optionId = optionIds.get(i);
                if (optionId == null) {
                    continue;
                }
                Option option = optionsById.get(optionId);
                if (option == null) {
                    continue;
                }
                if (categoryChanged && currentCategoryId != null
                        && (option.getCategory() == null || option.getCategory().getId() != currentCategoryId)) {
                    continue;
                }
                Value value = existingValuesByOptionId.computeIfAbsent(optionId, k -> {
                    Value newValue = new Value();
                    newValue.setProduct(existingProduct);
                    newValue.setOption(option);
                    return newValue;
                });
                String text = (values != null && i < values.size() && values.get(i) != null)
                        ? values.get(i).trim()
                        : "";
                value.setName(text);
                toSave.add(value);
            }
            if (!toSave.isEmpty()) {
                valueRepository.saveAll(toSave);
            }
        }

        productImageService.replaceImages(existingProduct, existingImages, imageFiles);
    }

    public Map<Option, Optional<Value>> getOptions(Product product) {
        Map<Option, Optional<Value>> result = new LinkedHashMap<>();
        if (product == null || product.getCategory() == null) {
            return result;
        }
        long categoryId = product.getCategory().getId();
        List<Option> options = optionRepository.findAllByCategoryId(categoryId);

        options.sort(Comparator.comparing(Option::getId));

        Map<Long, Value> valuesByOptionId = new HashMap<>();
        for (Value value : valueRepository.findByProduct(product)) {
            if (value.getOption() != null) {
                valuesByOptionId.putIfAbsent(value.getOption().getId(), value);
            }
        }

        for (Option option : options) {
            result.put(option, Optional.ofNullable(valuesByOptionId.get(option.getId())));
        }

        return result;
    }

    public Product getProductById(Long id) {
        if (id == null) return null;
        return productRepository.findByIdAndDeletedFalse(id).orElse(null);
    }

    public List<Product> findByPriceRangeAndCategory(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice) {
        if (categoryId == null) {
            return productRepository.findByPriceBetween(minPrice, maxPrice);
        } else {
            return productRepository.findByCategoryIdAndPriceBetween(categoryId, minPrice, maxPrice);
        }
    }

    public Page<Product> searchProducts(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return searchProducts(search, categoryId, null, minPrice, maxPrice, pageable);
    }

    public Page<Product> searchProducts(
            String search,
            Long categoryId,
            ProductAudience audience,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    ) {
        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return productRepository.searchProducts(query, categoryId, audience, minPrice, maxPrice, pageable);
    }
}

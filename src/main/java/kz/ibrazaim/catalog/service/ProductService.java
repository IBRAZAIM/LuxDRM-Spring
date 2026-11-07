package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.exception.EntityNotFoundException;
import kz.ibrazaim.catalog.model.*;
import kz.ibrazaim.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.HibernateException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService implements AbstractService<Product> {
    private final ProductRepository productRepository;
    private final ValueRepository valueRepository;
    private final OptionRepository optionRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageService productImageService;
    private final ProductSizeService productSizeService;
    private final ProductImageRepository productImageRepository;
    private final ReviewRepository reviewRepository;
    private final OrderProductRepository orderProductRepository;
    private final CartItemRepository cartItemRepository;

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
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("Товар с указанным id=" + productId + " не найден"));
        product.setDeleted(true);
        productRepository.save(product);
        cartItemRepository.deleteAllByProductId(productId);
    }

    @Transactional
    public void create(Product product, List<String> values, List<Long> optionsIds, Long categoryId, List<String> imageUrls, List<String> sizes) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        product.setCategory(category);

        // Устанавливаем рейтинг и скидку по умолчанию
        if (product.getRating() <= 0) {
            product.setRating(5.0);
        }
        if (product.getDiscount() == null) {
            product.setDiscount(BigDecimal.ZERO);
        }
        if (product.getStock() < 0) {
            product.setStock(0);
        }

        // Сохраняем продукт, чтобы получить ID
        Product savedProduct = productRepository.save(product);

        // Создаем характеристики
        if (optionsIds != null && !optionsIds.isEmpty()) {
            createValues(savedProduct, values != null ? values : List.of(), optionsIds);
        }

        // Создаем изображения
        if (imageUrls != null && !imageUrls.isEmpty()) {
            productImageService.create(savedProduct, imageUrls);
        }

        // Создаем размеры
        if (sizes != null && !sizes.isEmpty()) {
            productSizeService.saveSizes(savedProduct, sizes);
        }
    }

    private void createValues(Product product, List<String> values, List<Long> optionsIds) {
        List<Option> options = optionRepository.findAllById(optionsIds);
        List<Value> toSave = new ArrayList<>(options.size());

        for (int i = 0; i < options.size(); i++) {
            String valueText = (i < values.size()) ? values.get(i) : "";

            Value value = new Value();
            value.setName(valueText);
            value.setProduct(product);
            value.setOption(options.get(i));
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
    public void update(long id, String name, BigDecimal price, Integer stock, List<Long> optionIds, List<String> values) {
        Product existingProduct = findById(id);
        existingProduct.setName(name);
        existingProduct.setPrice(price);
        if (stock != null) {
            existingProduct.setStock(Math.max(0, stock));
        }

        productRepository.save(existingProduct);

        if (optionIds != null && values != null && !optionIds.isEmpty()) {
            Map<Long, Option> optionsById = new HashMap<>();
            for (Option option : optionRepository.findAllById(optionIds)) {
                optionsById.put(option.getId(), option);
            }
            Map<Long, Value> existingValuesByOptionId = new HashMap<>();
            for (Value v : valueRepository.findByProduct(existingProduct)) {
                if (v.getOption() != null) {
                    existingValuesByOptionId.put(v.getOption().getId(), v);
                }
            }
            List<Value> toSave = new ArrayList<>(optionIds.size());
            for (int i = 0; i < optionIds.size(); i++) {
                long optionId = optionIds.get(i);
                Option option = optionsById.get(optionId);
                if (option == null) {
                    throw new NoSuchElementException("Option not found: " + optionId);
                }
                Value value = existingValuesByOptionId.computeIfAbsent(optionId, k -> {
                    Value newValue = new Value();
                    newValue.setProduct(existingProduct);
                    newValue.setOption(option);
                    return newValue;
                });
                value.setName(i < values.size() ? values.get(i) : "");
                toSave.add(value);
            }
            valueRepository.saveAll(toSave);
        }
    }

    public Map<Option, Optional<Value>> getOptions(Product product) {
        Map<Option, Optional<Value>> result = new LinkedHashMap<>();
        long categoryId = product.getCategory().getId();
        List<Option> options = optionRepository.findAllByCategoryId(categoryId);

        options.sort(Comparator.comparing(Option::getId));

        Map<Long, Value> valuesByOptionId = new HashMap<>();
        for (Value value : valueRepository.findByProduct(product)) {
            if (value.getOption() != null) {
                valuesByOptionId.put(value.getOption().getId(), value);
            }
        }

        for (Option option : options) {
            Optional<Value> optionalValue = Optional.ofNullable(valuesByOptionId.get(option.getId()));
            if (optionalValue.isPresent() && result.containsValue(optionalValue)) {
                throw new HibernateException("Найдено более одной строки с данным идентификатором.");
            }
            result.put(option, optionalValue);
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
        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        return productRepository.searchProducts(query, categoryId, minPrice, maxPrice, pageable);
    }
}

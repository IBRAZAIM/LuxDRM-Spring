package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.exception.EntityNotFoundException;
import kz.ibrazaim.catalog.model.*;
import kz.ibrazaim.catalog.repository.*;
import lombok.RequiredArgsConstructor;
import org.hibernate.HibernateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Override
    public void create(Product product) {
        productRepository.save(product);
    }

    @Override
    public void deleteById(long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void deleteProductById(long productId) {
        productImageRepository.deleteByProductId(productId);
        valueRepository.deleteAllByProductId(productId);
        reviewRepository.deleteAllByProductId(productId);
        orderProductRepository.deleteAllByProductId(productId);
        deleteById(productId);
    }

    @Transactional
    public void create(Product product, List<String> values, List<Long> optionsIds, Long categoryId, List<String> imageUrls, List<String> sizes) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));

        product.setCategory(category);

        // Устанавливаем рейтинг и скидку по умолчанию
        product.setRating(5.0);      // рейтинг как double
        product.setDiscount(0);    // скидка в процентах, например 10% = 10.0

        // Сохраняем продукт, чтобы получить ID
        Product savedProduct = productRepository.save(product);

        // Создаем характеристики
        if (!optionsIds.isEmpty()) {
            createValues(savedProduct, values, optionsIds);
        }

        // Создаем изображения
        if (!imageUrls.isEmpty()) {
            productImageService.create(savedProduct, imageUrls);
        }

        // Создаем размеры
        if (sizes != null && !sizes.isEmpty()) {
            productSizeService.saveSizes(savedProduct, sizes);
        }
    }


    private void createValues(Product product, List<String> values, List<Long> optionsIds) {
        List<Option> options = optionRepository.findAllById(optionsIds);

        for (int i = 0; i < options.size(); i++) {
            // Проверка, чтобы индекс values не вышел за границы, если юзер что-то не ввел
            String valueText = (i < values.size()) ? values.get(i) : "";

            Value value = new Value();
            value.setName(valueText);
            value.setProduct(product);
            value.setOption(options.get(i));
            valueRepository.save(value);
        }
    }

    @Override
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Override
    public Product findById(long id) {
        return productRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Товар с указанным id=" + id + "не найден"));
    }

    public void update(long id, String name, int price, List<Long> optionIds, List<String> values) {
        Product existingProduct = findById(id);
        existingProduct.setName(name);
        existingProduct.setPrice(price);

        productRepository.save(existingProduct);

        for (int i = 0; i < optionIds.size(); i++) {
            long optionId = optionIds.get(i);
            Option option = optionRepository.findById(optionId).orElseThrow();
            Value value = valueRepository.findByProductAndOption(existingProduct, option).orElseGet(() -> {
                Value newValue = new Value();
                newValue.setProduct(existingProduct);
                newValue.setOption(option);
                return newValue;
            });
            value.setName(values.get(i));
            valueRepository.save(value);
        }
    }

    public Map<Option, Optional<Value>> getOptions(Product product) {
        Map<Option, Optional<Value>> result = new LinkedHashMap<>();
        long categoryId = product.getCategory().getId();
        List<Option> options = optionRepository.findAllByCategoryId(categoryId);

        options.sort(Comparator.comparing(Option::getId));

        for (Option option : options) {
            Optional<Value> optionalValue = valueRepository.findByProductAndOption(product, option);
            if (optionalValue.isPresent() && result.containsValue(optionalValue)) {
                throw new HibernateException("Найдено более одной строки с данным идентификатором.");
            }
            result.put(option, optionalValue);
        }

        return result;
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }

    public List<Product> findByPriceRangeAndCategory(Long categoryId, Integer minPrice, Integer maxPrice) {
        if (categoryId == null) {
            return productRepository.findByPriceBetween(minPrice, maxPrice);
        } else {
            return productRepository.findByCategoryIdAndPriceBetween(categoryId, minPrice, maxPrice);
        }
    }


}

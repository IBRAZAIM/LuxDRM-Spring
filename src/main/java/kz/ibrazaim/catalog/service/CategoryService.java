package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService implements AbstractService<Category> {
    private final CategoryRepository categoryRepository;
    private final OptionService optionService;

    @Override
    public void deleteProductById(long productId) {
    }

    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    @Transactional
    public void create(Category category) {
        if (category.getName() != null) {
            category.setName(category.getName().trim());
        }
        categoryRepository.save(category);
    }

    @Override
    public Category findById(long id) {
        return categoryRepository.findById(id).orElse(null);
    }

    @Transactional
    public void update(long id, String name, List<String> newOptionNames) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
        if (name != null && !name.trim().isEmpty()) {
            category.setName(name.trim());
            categoryRepository.save(category);
        }
        if (newOptionNames != null && !newOptionNames.isEmpty()) {
            optionService.create(newOptionNames, category);
        }
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категория не найдена"));
        List<Product> products = category.getProductList();
        if (products != null) {
            long activeCount = products.stream().filter(p -> !p.isDeleted()).count();
            if (activeCount > 0) {
                throw new IllegalStateException(
                        "Нельзя удалить категорию «" + category.getName() + "», так как в ней числится " + activeCount + " активных товаров."
                );
            }
        }
        optionService.deleteAllByCategoryId(id);
        categoryRepository.delete(category);
    }
}

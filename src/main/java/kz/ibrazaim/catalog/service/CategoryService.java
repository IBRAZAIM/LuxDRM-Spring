package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
@RequiredArgsConstructor
public class CategoryService implements AbstractService<Category> {
    private final CategoryRepository categoryRepository;
    @Override
    public List<Category> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public void create(Category category) {
        categoryRepository.save(category);
    }




    @Override
    public Category findById(long id) {
        return categoryRepository.findById(id).orElseThrow();
    }
    @Override
    public void update(long id, Category updatedEntity) {

    }

    @Override
    public void deleteById(long id) {

    }
}

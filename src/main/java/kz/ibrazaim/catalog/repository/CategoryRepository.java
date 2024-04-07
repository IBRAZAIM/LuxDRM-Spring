package kz.ibrazaim.catalog.repository;

import kz.ibrazaim.catalog.model.Category;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Component
public class CategoryRepository implements SimpleRepository<Category>  {
    private final Map<Long, Category> categories;
    private static long serial = 1;
    public CategoryRepository(){
        this.categories = new HashMap<>();
    }
    @Override
    public void create(Category category) {
        category.setId(getUniqueId());
        categories.put(category.getId(), category);
    }

    @Override
    public Category findById(long id) {
        return categories.get(id);
    }

    @Override
    public List<Category> findAll() {
        return new ArrayList<>(categories.values());
    }
    public long getUniqueId(){
        return serial ++;
    }
}



package kz.ibrazaim.catalog.repository;


import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.model.Product;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProductRepository implements SimpleRepository<Product> {

    private static long serial = 1;
    private Map<Long, Product> products;
    private CategoryRepository categoryRepository;
    private Product product;

    public ProductRepository() {
        this.products = new HashMap<>();
    }

    @Override
    public void create(Product product) {
        product.setId(getUniqueId());
        products.put(product.getId(), product);
    }

    @Override
    public Product findById(long id) {
        return products.get(id);
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }

    public long getUniqueId() {
        return serial++;
    }

}


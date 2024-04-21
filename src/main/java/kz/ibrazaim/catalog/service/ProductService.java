package kz.ibrazaim.catalog.service;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.model.Option;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Value;
import kz.ibrazaim.catalog.repository.CategoryRepository;
import kz.ibrazaim.catalog.repository.OptionRepository;
import kz.ibrazaim.catalog.repository.ProductRepository;
import kz.ibrazaim.catalog.repository.ValueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProductService implements AbstractService<Product>{
    private final ProductRepository productRepository;
    private final ValueRepository valueRepository;
    private final OptionRepository optionRepository;
    private final CategoryRepository categoryRepository;

    @Override
    public void create(Product product){
        productRepository.save(product);
    }


    public void create(Product product,List<String> values, List<Long> optionsIds, Long categoryId){
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        product.setCategory(category);
        productRepository.save(product);
        createValues(product, values, optionsIds);

    }
    private void createValues(Product product, List<String> values, List<Long> optionsIds) {
        if (values != null && optionsIds != null) {
            List<Option> options = optionRepository.findAllById(optionsIds);

            for (int i = 0; i < options.size(); i++) {
                Value value = new Value();
                value.setName(values.get(i));
                value.setProduct(product);
                value.setOption(options.get(i));
                valueRepository.save(value);
            }
        }
    }

    @Override
    public List<Product> findAll(){
        return productRepository.findAll();
    }

    @Override
    public Product findById(long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @Override
    public void update(long id, Product updatedProduct) {
        Product existingProduct = findById(id);
        existingProduct.setName(updatedProduct.getName());
        existingProduct.setPrice(updatedProduct.getPrice());

        productRepository.save(existingProduct);
    }

    @Override
    public void deleteById(long id) {
        productRepository.deleteById(id);
    }
}

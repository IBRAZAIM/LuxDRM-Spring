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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    @Override
    public void update(long id, Category updatedEntity) {

    }


    public void create(Product product,List<String> values, List<Long> optionsIds, Long categoryId){
        Category category = categoryRepository.findById(categoryId).orElseThrow();
        product.setCategory(category);
        productRepository.save(product);
        createValues(product, values, optionsIds);

    }
    private void createValues(Product product, List<String> values, List<Long> optionsIds) {
        List<Option> options = optionRepository.findAllById(optionsIds);

        for (int i = 0; i < options.size(); i++) {
            Value value = new Value();
            value.setName(values.get(i));
            value.setProduct(product);
            value.setOption(options.get(i));
            valueRepository.save(value);
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
    public void deleteById(long id) {
        valueRepository.deleteAllByProductId(id);
        productRepository.deleteById(id);
    }

    public void update(long id, String name, int price, List<Long> optionIds, List<String> values) {
        Product existingProduct = findById(id);
        existingProduct.setName(name);
        existingProduct.setPrice(price);
        List<Value> valueList = existingProduct.getValueList();
        List<Option> options = optionRepository.findAllById(optionIds);

        for (Option option : options) {
            valueRepository.findByProductAndOption(pr);
        }

        if (values.size() > valueList.size()) {
            throw new RuntimeException("Размер списка новых значений больше размера текущего списка значений");
        }
        for (int i = 0; i < options.size(); i++) {
            if (i < values.size()) {
                String valueName = values.get(i);
                Value existingValue = valueList.get(i);
                existingValue.setName(valueName);
            } else {
                break;
            }
        }
        productRepository.save(existingProduct);
    }

    public Map<Option, Optional<Value>> getOptions(Product product){
        Map<Option, Optional<Value>> result = new LinkedHashMap<>();
        long categoryId = product.getCategory().getId();
        List<Option> options = optionRepository.findAllByCategoryId(categoryId);
        for (Option option : options) {
            Optional<Value> optionalValue = valueRepository.findByProductAndOption(product, option);
            result.put(option, optionalValue);
        }
        return result;
    }


    public Product getProductById(Long id) {
        return productRepository.findById(id).orElse(null);
    }
}

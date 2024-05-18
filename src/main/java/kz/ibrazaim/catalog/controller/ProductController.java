package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.service.CategoryService;
import kz.ibrazaim.catalog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    @GetMapping
    public String findAll(Model model){
        model.addAttribute("products", productService.findAll());
        return "product-list";
    }
    @GetMapping("/create")
    public String showProductForm(Model model, @RequestParam long categoryId) {
        model.addAttribute("product", new Product());
        model.addAttribute("category", categoryService.findById(categoryId));
        return "productForm";
    }
    @PostMapping("/create")
    public String showProductForm(
            @ModelAttribute("product") Product product,
            @RequestParam(required = false) List<String> values,
            @RequestParam(required = false) List<Long> optionsIds,
            @RequestParam(defaultValue = "-1") Long categoryId,
            Model model)
    {
        if (categoryId == -1){
            return "redirect:/products/create/chooseCategory";
        }
        productService.create(product);
        model.addAttribute("options", optionsIds);
        productService.create(product, values, optionsIds, categoryId);

        return "redirect:/products";
    }
    @GetMapping("create/chooseCategory")
    public String chooseCategory(Model model){
        model.addAttribute("categories", categoryService.findAll());
        return "chooseCategory";
    }

    @GetMapping("/update/{id}")
    public String showUpdateProductForm(@PathVariable("id") long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("options", productService.getOptions(product));
        return "update-product";
    }

    @PostMapping("/update/{id}")
    public String updateProduct(@PathVariable long id, String name, int price, @RequestParam List<Long> optionIds, @RequestParam List<String> values) {
        productService.update(id, name, price, optionIds, values);
        return "redirect:/products";
    }

    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(@PathVariable long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "product-delete";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable long id) {
        productService.deleteById(id);
        return "redirect:/products";
    }
    @GetMapping("/{id}")
    public String productCard(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("options", productService.getOptions(product));
        return "product-page";
    }


    @GetMapping("/filter")
    public String filterProducts(@RequestParam(value = "minPrice", required = false) Double minPrice,
                                 @RequestParam(value = "maxPrice", required = false) Double maxPrice,
                                 Model model) {
        List<Product> products = productService.findByPriceRange(minPrice, maxPrice);
        model.addAttribute("products", products);
        return "product-list";
    }

}

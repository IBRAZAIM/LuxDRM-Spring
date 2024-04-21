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

    @GetMapping("/update")
    public String showUpdateForm(@RequestParam long id, Model model) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "update-product";
    }

    @PostMapping("/update")
    public String updateProduct(@RequestParam long id, @ModelAttribute("product") Product updatedProduct) {
        productService.update(id, updatedProduct);
        return "redirect:/products";
    }

    @GetMapping("update/chooseProduct")
    public String chooseProduct(Model model){
        model.addAttribute("products", productService.findAll());
        return "chooseProduct";
    }
}

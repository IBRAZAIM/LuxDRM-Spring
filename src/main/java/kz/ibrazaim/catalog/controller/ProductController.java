package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.repository.CategoryRepository;
import kz.ibrazaim.catalog.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/products")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @GetMapping
    public String findAll(Model model){
        model.addAttribute("products", productRepository.findAll());
        return "product-list";
    }
    @GetMapping("/create")
    public String showProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("products", productRepository.findAll());
        model.addAttribute("categories", categoryRepository.findAll());
        return "productForm";
    }

    @PostMapping("/create")
    public String addProduct(@ModelAttribute("product") Product product) {
        productRepository.create(product);
        return "redirect:/products/create";
    }
}

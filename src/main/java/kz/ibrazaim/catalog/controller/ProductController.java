package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final ProductImageService productImageService;

    @GetMapping
    public String findAll(
            @RequestParam(defaultValue = "0") Integer minPrice,
            @RequestParam(defaultValue = "" + Integer.MAX_VALUE) Integer maxPrice,
            @RequestParam(required = false) Long categoryId,
            Model model
    ) {
        User user = userService.getUser();
        List<Product> products;
        if (minPrice == 0 && maxPrice == Integer.MAX_VALUE && categoryId == null) {
            products = productService.findAll();
        } else {
            products = productService.findByPriceRangeAndCategory(categoryId, minPrice, maxPrice);
        }
        model.addAttribute("products", products);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        if (user != null && user.getRole().equals(Role.ADMIN.getServiceName())) {
            return "productsByAdmin";
        }
        return "productsByUser";
    }

    @GetMapping("/create")
    public String showProductForm(
            Model model,
            @RequestParam long categoryId
    ) {
        model.addAttribute("product", new Product());
        model.addAttribute("category", categoryService.findById(categoryId));
        return "productForm";
    }

    @PostMapping("/create")
    public String createProduct(
            @ModelAttribute("product") Product product,
            @RequestParam(required = false) List<String> values,
            @RequestParam("optionsIds[]") List<Long> optionsIds,
            @RequestParam(defaultValue = "-1") Long categoryId,
            @RequestParam("imageUrls[]") List<String> imageUrls,
            @RequestParam("sizes") List<String> sizes
    ) {
        if (categoryId == -1) {
            return "redirect:/products/create/chooseCategory";
        }
//        System.out.println("optionsIds: " + optionsIds);
//        for (String size : sizes){
//            System.out.println("size: " + size);
//        }
//        System.out.println("Image URLs: " + imageUrls);
        productService.create(product, values, optionsIds, categoryId, imageUrls, sizes);
        return "redirect:/products";
    }

    @GetMapping("create/chooseCategory")
    public String chooseCategory(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "chooseCategory";
    }

    @GetMapping("/update/{id}")
    public String showUpdateProductForm(
            @PathVariable("id") long id,
            Model model
    ) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("options", productService.getOptions(product));
        return "update-product";
    }

    @PostMapping("/update/{id}")
    public String updateProduct(
            @PathVariable long id,
            String name, int price,
            @RequestParam List<Long> optionIds,
            @RequestParam List<String> values
    ) {
        productService.update(id, name, price, optionIds, values);
        return "redirect:/products";
    }

    @GetMapping("/delete/{id}")
    public String showDeleteConfirmation(
            @PathVariable long id,
            Model model
    ) {
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        return "product-delete";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable long id) {
        productService.deleteProductById(id);
        return "redirect:/products";
    }

    @GetMapping("/{id}")
    public String productCard(
            @PathVariable Long id,
            Model model
    ) {
        Product product = productService.getProductById(id);
        model.addAttribute("product", product);
        model.addAttribute("options", productService.getOptions(product));
        model.addAttribute("reviews", reviewService.getCommentsForProduct(product));
        model.addAttribute("imageUrl", productImageService.findByProduct(product));
        User user = userService.getUser();
        // Проверка на авторизацию пользователя
        if (user != null) {
            model.addAttribute("user", user);
        } else {
            // Создание пустого пользователя для неавторизованных пользователей
            model.addAttribute("user", new User());
        }
        return "product-page";
    }

    @PostMapping("/addComment")
    public String addReview(
            @RequestParam("productId") Long productId,
            @RequestParam("comment") String commentText,
            @RequestParam("estimation") int estimation
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        Product product = productService.getProductById(productId);
        reviewService.create(product, user, estimation, commentText);

        return "redirect:/products/" + productId;
    }
}

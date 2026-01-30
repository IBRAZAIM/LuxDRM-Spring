package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.exception.ProductCreateException;
import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductSize;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private final ProductSizeService productSizeService;
    private final CartService cartService;

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
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount());
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
            @RequestParam(value = "values", required = false) List<String> values,
            @RequestParam(value = "optionsIds", required = false) List<Long> optionsIds,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "imageUrls[]", required = false) List<String> imageUrls,
            @RequestParam(value = "sizes[]", required = false) List<String> sizes,
            RedirectAttributes redirectAttributes
    ) {
        try {
            productService.create(
                    product,
                    values != null ? values : List.of(),
                    optionsIds != null ? optionsIds : List.of(),
                    categoryId,
                    imageUrls != null ? imageUrls : List.of(),
                    sizes != null ? sizes : List.of()
            );
            return "redirect:/products";
        } catch (Exception e) {
            // Логируем ошибку, чтобы видеть её в консоли
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Ошибка при создании: " + e.getMessage());
            return "redirect:/products/create?categoryId=" + categoryId;
        }
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
        int cartItemsCount = cartService.getCartItemsCount(); // Метод для получения количества товаров в корзине
        model.addAttribute("cartItemsCount", cartItemsCount);
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
        model.addAttribute("products", productService.findAll());
        model.addAttribute("options", productService.getOptions(product));
        model.addAttribute("reviews", reviewService.getCommentsForProduct(product));
        model.addAttribute("imageUrls", productImageService.findAllByProduct(product));
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount());
        List<ProductSize> sizes = productSizeService.findByProduct(product);
        model.addAttribute("sizes", sizes);

        User user = userService.getUser();
        if (user != null) {
            model.addAttribute("user", user);
        } else {
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

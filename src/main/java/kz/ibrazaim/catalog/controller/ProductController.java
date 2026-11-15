package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductSize;
import kz.ibrazaim.catalog.model.Role;
import kz.ibrazaim.catalog.model.User;
import kz.ibrazaim.catalog.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final UserService userService;
    private final ProductImageService productImageService;
    private final ProductSizeService productSizeService;
    private final CartService cartService;

    @InitBinder("product")
    public void initProductBinder(WebDataBinder binder) {
        binder.setDisallowedFields("sizes", "images", "category", "valueList");
    }

    @GetMapping
    public String findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            jakarta.servlet.http.HttpSession session,
            Model model
    ) {
        User user = userService.getUser();

        if (minPrice == null) minPrice = BigDecimal.ZERO;
        if (maxPrice == null) maxPrice = new BigDecimal("9999999999.99");
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 12;

        if (userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
            Page<Product> productsPage = productService.searchProducts(search, categoryId, minPrice, maxPrice, pageable);

            model.addAttribute("products", productsPage.getContent());
            model.addAttribute("productsPage", productsPage);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productsPage.getTotalPages());
            model.addAttribute("totalElements", productsPage.getTotalElements());
            model.addAttribute("search", search != null ? search.trim() : "");
            model.addAttribute("minPrice", minPrice);
            model.addAttribute("maxPrice", maxPrice);
            model.addAttribute("categories", categoryService.findAll());
            model.addAttribute("selectedCategoryId", categoryId);
            model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
            return "productsByAdmin";
        }

        List<Product> allProducts = productService.findAll();
        model.addAttribute("products", allProducts);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
        return "productsByUser";
    }

    @GetMapping("/create")
    public String showProductForm(
            Model model,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            RedirectAttributes redirectAttributes
    ) {
        if (categoryId == null) {
            return "redirect:/products/create/chooseCategory";
        }
        var category = categoryService.findById(categoryId);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Выбранная категория не найдена");
            return "redirect:/products/create/chooseCategory";
        }
        model.addAttribute("product", new Product());
        model.addAttribute("category", category);
        return "productForm";
    }

    @PostMapping("/create")
    public String createProduct(
            @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            @RequestParam(value = "values", required = false) List<String> values,
            @RequestParam(value = "optionsIds", required = false) List<Long> optionsIds,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            @RequestParam(value = "sizes[]", required = false) List<String> sizesBracket,
            @RequestParam(value = "sizes", required = false) List<String> sizesPlain,
            RedirectAttributes redirectAttributes
    ) {
        if (categoryId == null && product.getCategory() != null) {
            categoryId = product.getCategory().getId();
        }
        if (categoryId == null) {
            redirectAttributes.addFlashAttribute("error", "Категория товара не выбрана");
            return "redirect:/products/create/chooseCategory";
        }

        if (bindingResult.hasErrors()) {
            StringBuilder sb = new StringBuilder();
            bindingResult.getFieldErrors().forEach(fe -> {
                if (!"sizes".equals(fe.getField()) && !"images".equals(fe.getField())) {
                    sb.append(fe.getField()).append(": ").append(fe.getDefaultMessage()).append("; ");
                }
            });
            if (sb.length() > 0) {
                redirectAttributes.addFlashAttribute("error", "Ошибка заполнения: " + sb);
                return "redirect:/products/create?categoryId=" + categoryId;
            }
        }

        List<String> combinedSizes = new ArrayList<>();
        if (sizesBracket != null) combinedSizes.addAll(sizesBracket);
        if (sizesPlain != null) combinedSizes.addAll(sizesPlain);

        try {
            productService.create(
                    product,
                    values != null ? values : List.of(),
                    optionsIds != null ? optionsIds : List.of(),
                    categoryId,
                    imageFiles != null ? imageFiles : List.of(),
                    combinedSizes
            );
            return "redirect:/products";
        } catch (Exception e) {
            logger.error("Ошибка при создании товара", e);
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
            jakarta.servlet.http.HttpSession session,
            Model model
    ) {
        User user = userService.getUser();
        Product product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("options", productService.getOptions(product));
        model.addAttribute("sizes", productSizeService.findByProduct(product));
        model.addAttribute("images", productImageService.findAllByProduct(product));
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
        return "update-product";
    }

    @PostMapping("/update/{id}")
    public String updateProduct(
            @PathVariable long id,
            @RequestParam String name,
            @RequestParam BigDecimal price,
            @RequestParam(value = "discount", required = false, defaultValue = "0") BigDecimal discount,
            @RequestParam(value = "stock", required = false, defaultValue = "100") Integer stock,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "optionIds", required = false) List<Long> optionIds,
            @RequestParam(value = "values", required = false) List<String> values,
            @RequestParam(value = "sizes[]", required = false) List<String> sizesBracket,
            @RequestParam(value = "sizes", required = false) List<String> sizesPlain,
            @RequestParam(value = "existingImages", required = false) List<String> existingImages,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            RedirectAttributes redirectAttributes
    ) {
        List<String> combinedSizes = new ArrayList<>();
        if (sizesBracket != null) combinedSizes.addAll(sizesBracket);
        if (sizesPlain != null) combinedSizes.addAll(sizesPlain);

        try {
            productService.update(
                    id,
                    name,
                    price,
                    discount,
                    stock,
                    description,
                    optionIds,
                    values,
                    combinedSizes,
                    existingImages != null ? existingImages : List.of(),
                    imageFiles != null ? imageFiles : List.of()
            );
            return "redirect:/products";
        } catch (Exception e) {
            logger.error("Ошибка при обновлении товара id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/products/update/" + id;
        }
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
            jakarta.servlet.http.HttpSession session,
            Model model
    ) {
        User user = userService.getUser();
        Product product = productService.getProductById(id);
        if (product == null) {
            return "redirect:/products";
        }
        model.addAttribute("product", product);
        model.addAttribute("products", productService.findSimilarProducts(product));
        model.addAttribute("options", productService.getOptions(product));
        model.addAttribute("reviews", reviewService.getCommentsForProduct(product));
        model.addAttribute("imageUrls", productImageService.findAllByProduct(product));
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));

        List<ProductSize> sizes = productSizeService.findByProduct(product);
        model.addAttribute("sizes", sizes);

        if (user != null) {
            model.addAttribute("user", user);
        } else {
            model.addAttribute("user", new User());
        }
        return "product-page";
    }

    @GetMapping("/api/search")
    @ResponseBody
    public List<Map<String, Object>> liveSearch(@RequestParam(value = "q", required = false) String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        String query = q.trim();
        Pageable topSix = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id"));
        List<Product> matches = productService.searchProducts(
                query,
                null,
                BigDecimal.ZERO,
                new BigDecimal("9999999999.99"),
                topSix
        ).getContent();

        List<Map<String, Object>> items = new ArrayList<>(matches.size());
        for (Product p : matches) {
            var images = productImageService.findAllByProduct(p);
            String imageUrl = (images != null && !images.isEmpty() && images.get(0) != null)
                    ? images.get(0).getImage()
                    : "";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("name", p.getName() != null ? p.getName() : "");
            row.put("category", p.getCategory() != null ? p.getCategory().getName() : "LUXDRM");
            row.put("price", p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO);
            row.put("finalPrice", p.getFinalPrice());
            row.put("discount", p.getDiscount() != null ? p.getDiscount() : BigDecimal.ZERO);
            row.put("image", imageUrl);
            items.add(row);
        }
        return items;
    }

    @PostMapping("/addComment")
    public String addReview(
            @RequestParam("productId") Long productId,
            @RequestParam("comment") String commentText,
            @RequestParam("estimation") int estimation,
            RedirectAttributes redirectAttributes
    ) {
        User user = userService.getUser();
        if (user == null) {
            return "redirect:/login";
        }
        Product product = productService.getProductById(productId);
        reviewService.create(product, user, estimation, commentText);
        redirectAttributes.addFlashAttribute("reviewSuccess", "Спасибо! Ваш отзыв отправлен на модерацию.");

        return "redirect:/products/" + productId;
    }
}

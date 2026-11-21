package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Product;
import kz.ibrazaim.catalog.model.ProductAudience;
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
            @RequestParam(required = false) String audience,
            @RequestParam(required = false) String section,
            @RequestParam(defaultValue = "newest") String sort,
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

        String rawAudience = (audience != null && !audience.isBlank()) ? audience : section;
        ProductAudience parsedAudience = ProductAudience.fromSlugOrName(rawAudience);

        if (userService.hasAnyRole(user, Role.ADMIN, Role.MODER)) {
            Sort jpaSort = switch (sort != null ? sort : "newest") {
                case "price_asc" -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
                case "price_desc" -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"));
                case "stock_asc" -> Sort.by(Sort.Direction.ASC, "stock").and(Sort.by(Sort.Direction.DESC, "id"));
                case "stock_desc" -> Sort.by(Sort.Direction.DESC, "stock").and(Sort.by(Sort.Direction.DESC, "id"));
                default -> Sort.by(Sort.Direction.DESC, "id");
            };
            Pageable pageable = PageRequest.of(page, size, jpaSort);
            Page<Product> productsPage = productService.searchProducts(search, categoryId, parsedAudience, minPrice, maxPrice, pageable);

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
            model.addAttribute("audiences", ProductAudience.values());
            model.addAttribute("selectedAudience", parsedAudience != null ? parsedAudience.name() : "");
            model.addAttribute("selectedSort", sort);
            model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
            return "productsByAdmin";
        }

        List<Product> allProducts = productService.findAll();
        model.addAttribute("products", allProducts);
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedSection", parsedAudience != null && parsedAudience != ProductAudience.UNISEX ? parsedAudience.getSlug() : "");
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
        return "productsByUser";
    }

    @GetMapping("/create")
    public String showProductForm(
            Model model,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            RedirectAttributes redirectAttributes
    ) {
        var categories = categoryService.findAll();
        if (categories.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Сначала создайте хотя бы одну категорию товара");
            return "redirect:/categories/create";
        }
        if (categoryId == null) {
            categoryId = categories.get(0).getId();
        }
        var category = categoryService.findById(categoryId);
        if (category == null) {
            redirectAttributes.addFlashAttribute("error", "Выбранная категория не найдена");
            category = categories.get(0);
        }
        model.addAttribute("product", new Product());
        model.addAttribute("category", category);
        model.addAttribute("categories", categories);
        model.addAttribute("audiences", ProductAudience.values());
        return "productForm";
    }

    @PostMapping("/create")
    public String createProduct(
            @ModelAttribute("product") Product product,
            BindingResult bindingResult,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            jakarta.servlet.http.HttpServletRequest request,
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

        List<String> values = extractRawParams(request, "values");
        List<Long> optionsIds = extractLongParams(request, "optionsIds", "optionIds");
        List<String> combinedSizes = extractRawParams(request, "sizes[]", "sizes");
        List<Integer> sizeStocks = extractIntegerParams(request, "sizeStocks[]", "sizeStocks");

        try {
            productService.create(
                    product,
                    values,
                    optionsIds,
                    categoryId,
                    imageFiles != null ? imageFiles : List.of(),
                    combinedSizes,
                    sizeStocks
            );
            String cleanName = product.getName() != null ? product.getName().trim() : "";
            redirectAttributes.addFlashAttribute("success", "Товар «" + cleanName + "» успешно создан");
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
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("audiences", ProductAudience.values());
        model.addAttribute("options", productService.getOptions(product));
        model.addAttribute("sizes", productSizeService.findByProduct(product));
        model.addAttribute("images", productImageService.findAllByProduct(product));
        model.addAttribute("cartItemsCount", cartService.getCartItemsCount(user, session));
        return "update-product";
    }

    @PostMapping("/update/{id}")
    public String updateProduct(
            @PathVariable long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "sku", required = false) String sku,
            @RequestParam(value = "audience", required = false) String audienceRaw,
            @RequestParam(value = "price", required = false) String priceRaw,
            @RequestParam(value = "discount", required = false) String discountRaw,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
            jakarta.servlet.http.HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            BigDecimal price = parseBigDecimal(priceRaw, null);
            BigDecimal discount = parseBigDecimal(discountRaw, BigDecimal.ZERO);
            ProductAudience audience = ProductAudience.fromSlugOrName(audienceRaw);

            List<Long> optionIds = extractLongParams(request, "optionIds", "optionsIds");
            List<String> values = extractRawParams(request, "values");
            List<String> combinedSizes = extractRawParams(request, "sizes[]", "sizes");
            List<Integer> sizeStocks = extractIntegerParams(request, "sizeStocks[]", "sizeStocks");
            List<String> existingImages = extractRawParams(request, "existingImages");

            productService.update(
                    id,
                    name,
                    sku,
                    audience,
                    price,
                    discount,
                    description,
                    categoryId,
                    optionIds,
                    values,
                    combinedSizes,
                    sizeStocks,
                    existingImages,
                    imageFiles != null ? imageFiles : List.of()
            );
            String cleanName = name != null ? name.trim() : "";
            redirectAttributes.addFlashAttribute("success", "Товар «" + cleanName + "» успешно обновлён");
            return "redirect:/products";
        } catch (Exception e) {
            logger.error("Ошибка при обновлении товара id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Ошибка при обновлении: " + e.getMessage());
            return "redirect:/products/update/" + id;
        }
    }

    private List<String> extractRawParams(jakarta.servlet.http.HttpServletRequest request, String... paramNames) {
        List<String> result = new ArrayList<>();
        if (request == null || paramNames == null) {
            return result;
        }
        for (String paramName : paramNames) {
            String[] rawValues = request.getParameterValues(paramName);
            if (rawValues != null) {
                for (String val : rawValues) {
                    result.add(val != null ? val : "");
                }
            }
        }
        return result;
    }

    private List<Integer> extractIntegerParams(jakarta.servlet.http.HttpServletRequest request, String... paramNames) {
        List<Integer> result = new ArrayList<>();
        for (String raw : extractRawParams(request, paramNames)) {
            String trimmed = raw.trim().replace("\u00A0", "").replace(" ", "");
            if (trimmed.isEmpty()) {
                result.add(0);
            } else {
                try {
                    result.add(Math.max(0, Integer.parseInt(trimmed)));
                } catch (NumberFormatException ignored) {
                    result.add(0);
                }
            }
        }
        return result;
    }

    private List<Long> extractLongParams(jakarta.servlet.http.HttpServletRequest request, String... paramNames) {
        List<Long> result = new ArrayList<>();
        for (String raw : extractRawParams(request, paramNames)) {
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                try {
                    result.add(Long.parseLong(trimmed));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return result;
    }

    private BigDecimal parseBigDecimal(String raw, BigDecimal defaultVal) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultVal;
        }
        String normalized = raw.trim()
                .replace("\u00A0", "")
                .replace(" ", "")
                .replace(",", ".");
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректное числовое значение: " + raw);
        }
    }

    private Integer parseInteger(String raw, Integer defaultVal) {
        if (raw == null || raw.trim().isEmpty()) {
            return defaultVal;
        }
        String normalized = raw.trim()
                .replace("\u00A0", "")
                .replace(" ", "");
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некорректное целое значение: " + raw);
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
    public String deleteProduct(@PathVariable long id, RedirectAttributes redirectAttributes) {
        ProductService.DeleteResult result = productService.deleteProductWithResult(id);
        if (result.hardDeleted()) {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Товар «" + result.productName() + "» полностью удалён из базы данных"
            );
        } else {
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Товар «" + result.productName() + "» скрыт из каталога (оставлен в БД для истории заказов)"
            );
        }
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
            var images = p.getImages();
            String imageUrl = (images != null && !images.isEmpty() && images.get(0) != null && images.get(0).getImage() != null)
                    ? images.get(0).getImage()
                    : "";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", p.getId());
            row.put("sku", p.getDisplaySku());
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

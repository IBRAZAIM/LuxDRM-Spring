package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.service.CategoryService;
import kz.ibrazaim.catalog.service.OptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryService categoryService;
    private final OptionService optionService;

    @GetMapping
    public String findAll(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "categories-list";
    }

    @GetMapping("/create")
    public String showForm(Model model) {
        model.addAttribute("category", new Category());
        model.addAttribute("isEdit", false);
        return "categoryForm";
    }

    @PostMapping("/create")
    public String createPost(
            @ModelAttribute Category category,
            @RequestParam(value = "optionNames", required = false) List<String> optionNames,
            RedirectAttributes redirectAttributes
    ) {
        if (category.getName() == null || category.getName().trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Название категории не может быть пустым");
            return "redirect:/categories/create";
        }
        categoryService.create(category);
        if (optionNames != null && !optionNames.isEmpty()) {
            optionService.create(optionNames, category);
        }
        redirectAttributes.addFlashAttribute("successMessage", "Категория «" + category.getName() + "» успешно создана");
        return "redirect:/categories";
    }

    @GetMapping("/update/{id}")
    public String showEditForm(@PathVariable long id, Model model, RedirectAttributes redirectAttributes) {
        Category category = categoryService.findById(id);
        if (category == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Категория не найдена");
            return "redirect:/categories";
        }
        model.addAttribute("category", category);
        model.addAttribute("existingOptions", optionService.findAllByCategoryId(id));
        model.addAttribute("isEdit", true);
        return "categoryForm";
    }

    @PostMapping("/update/{id}")
    public String updateCategory(
            @PathVariable long id,
            @RequestParam("name") String name,
            @RequestParam(value = "optionNames", required = false) List<String> optionNames,
            RedirectAttributes redirectAttributes
    ) {
        try {
            categoryService.update(id, name, optionNames);
            redirectAttributes.addFlashAttribute("successMessage", "Категория успешно обновлена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/categories";
    }

    @PostMapping("/delete/{id}")
    public String deleteCategory(@PathVariable long id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Категория успешно удалена");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/categories";
    }
}

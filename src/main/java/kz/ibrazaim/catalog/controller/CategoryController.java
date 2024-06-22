package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.service.CategoryService;
import kz.ibrazaim.catalog.service.OptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        return "categoryForm";
    }

    @PostMapping("/create")
    public String createPost(
            @ModelAttribute Category category,
            @RequestParam List<String> optionNames,
            Model model
    ) {
        categoryService.create(category);
        if (optionNames != null) {
            model.addAttribute("optionNames",optionNames);
            optionService.create(optionNames, category);
        }
        return "redirect:/categories";
    }
}


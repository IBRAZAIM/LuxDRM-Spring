package kz.ibrazaim.catalog.controller;

import kz.ibrazaim.catalog.model.Category;
import kz.ibrazaim.catalog.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {
    private final CategoryRepository categoryRepository;

    @GetMapping
    public String findAll(Model model){
        model.addAttribute("categories", categoryRepository.findAll());
        return "categories-list";
    }

    @GetMapping("/create")
    public String createGet(Model model){
        model.addAttribute("category", new Category());
        return "categoryForm";
    }

    @PostMapping("/create")
    public String createCategory(@ModelAttribute("category") Category category) {
        categoryRepository.create(category);
        return "redirect:/categories/create";
    }
}

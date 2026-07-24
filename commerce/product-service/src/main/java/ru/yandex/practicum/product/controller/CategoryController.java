package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.service.CategoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

	private final CategoryService categoryService;

	@GetMapping
	public List<CategoryDto> getAllCategories() {
		log.info("GET /api/categories");
		return categoryService.getAllCategories();
	}

	@GetMapping("/{id}")
	public CategoryDto getCategoryById(@PathVariable Long id) {
		log.info("GET /api/categories/{}", id);
		return categoryService.getCategoryById(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CategoryDto createCategory(@Valid @RequestBody CreateCategoryRequest request) {
		log.info("POST /api/categories");
		return categoryService.createCategory(request);
	}
}
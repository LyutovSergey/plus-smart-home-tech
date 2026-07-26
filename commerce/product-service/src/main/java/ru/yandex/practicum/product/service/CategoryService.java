package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.CategoryNotFoundException;
import ru.yandex.practicum.product.mapper.CategoryMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	public List<CategoryDto> getAllCategories() {
		log.info("Fetching all categories");
		return categoryRepository.findAll().stream()
				.map(categoryMapper::toDto)
				.collect(Collectors.toList());
	}

	public CategoryDto getCategoryById(Long id) {
		log.info("Fetching category by id: {}", id);
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new CategoryNotFoundException(id));
		return categoryMapper.toDto(category);
	}

	@Transactional
	public CategoryDto createCategory(CreateCategoryRequest request) {
		log.info("Creating new category: {}", request.name());
		Category category = categoryMapper.toEntity(request);
		Category saved = categoryRepository.save(category);
		log.info("Category created with id: {}", saved.getId());
		return categoryMapper.toDto(saved);
	}

	public Category getCategoryEntity(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new CategoryNotFoundException(id));
	}
}
package ru.yandex.practicum.product.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;

@Component
public class CategoryMapper {

	public Category toEntity(CreateCategoryRequest request) {
		return new Category(request.name(), request.description());
	}

	public CategoryDto toDto(Category category) {
		if (category == null) {
			return null;
		}
		return new CategoryDto(
				category.getId(),
				category.getName(),
				category.getDescription()
		);
	}
}
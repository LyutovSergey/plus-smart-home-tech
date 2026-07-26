package ru.yandex.practicum.product.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;

@Component
@RequiredArgsConstructor
public class ProductMapper {

	private final CategoryMapper categoryMapper;

	public Product toEntity(CreateProductRequest request, Category category) {
		return new Product(
				request.name(),
				request.description(),
				request.price(),
				request.imageUrl(),
				category
		);
	}

	public ProductDto toDto(Product product) {
		if (product == null) {
			return null;
		}
		return new ProductDto(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getPrice(),
				categoryMapper.toDto(product.getCategory()),
				product.getImageUrl(),
				product.getActive()
		);
	}

	public void updateEntity(Product product, UpdateProductRequest request, Category category) {
		if (request.name() != null) {
			product.setName(request.name());
		}
		if (request.description() != null) {
			product.setDescription(request.description());
		}
		if (request.price() != null) {
			product.setPrice(request.price());
		}
		if (request.imageUrl() != null) {
			product.setImageUrl(request.imageUrl());
		}
		if (request.active() != null) {
			product.setActive(request.active());
		}
		if (category != null) {
			product.setCategory(category);
		}
	}
}
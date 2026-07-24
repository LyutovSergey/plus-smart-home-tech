package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.ProductNotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

	private final ProductRepository productRepository;
	private final CategoryService categoryService;
	private final ProductMapper productMapper;

	public List<ProductDto> getAllActiveProducts() {
		log.info("Fetching all active products");
		return productRepository.findByActiveTrue().stream()
				.map(productMapper::toDto)
				.collect(Collectors.toList());
	}

	public ProductDto getProductById(Long id) {
		log.info("Fetching product by id: {}", id);
		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));
		return productMapper.toDto(product);
	}

	public List<ProductDto> getProductsByCategory(Long categoryId) {
		log.info("Fetching products for category id: {}", categoryId);
		return productRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
				.map(productMapper::toDto)
				.collect(Collectors.toList());
	}

	public List<ProductDto> searchProducts(String query) {
		log.info("Searching products by query: {}", query);
		return productRepository.searchByName(query).stream()
				.map(productMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public ProductDto createProduct(CreateProductRequest request) {
		log.info("Creating new product: {}", request.name());

		Category category = null;
		if (request.categoryId() != null) {
			category = categoryService.getCategoryEntity(request.categoryId());
		}

		Product product = productMapper.toEntity(request, category);
		Product saved = productRepository.save(product);
		log.info("Product created with id: {}", saved.getId());
		return productMapper.toDto(saved);
	}

	@Transactional
	public ProductDto updateProduct(Long id, UpdateProductRequest request) {
		log.info("Updating product id: {}", id);

		Product product = productRepository.findById(id)
				.orElseThrow(() -> new ProductNotFoundException(id));

		Category category = null;
		if (request.categoryId() != null) {
			category = categoryService.getCategoryEntity(request.categoryId());
		}

		productMapper.updateEntity(product, request, category);
		Product updated = productRepository.save(product);
		log.info("Product updated: {}", updated.getId());
		return productMapper.toDto(updated);
	}
}
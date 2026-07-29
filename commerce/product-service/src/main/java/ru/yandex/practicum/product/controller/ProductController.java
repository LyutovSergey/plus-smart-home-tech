package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.service.ProductService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;

	@GetMapping
	public List<ProductDto> getAllProducts() {
		log.info("GET /api/products");
		return productService.getAllActiveProducts();
	}

	@GetMapping("/{id}")
	public ProductDto getProductById(@PathVariable Long id) {
		log.info("GET /api/products/{}", id);
		return productService.getProductById(id);
	}

	@GetMapping("/category/{categoryId}")
	public List<ProductDto> getProductsByCategory(@PathVariable Long categoryId) {
		log.info("GET /api/products/category/{}", categoryId);
		return productService.getProductsByCategory(categoryId);
	}

	@GetMapping("/search")
	public List<ProductDto> searchProducts(@RequestParam String query) {
		log.info("GET /api/products/search?query={}", query);
		return productService.searchProducts(query);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductDto createProduct(@Valid @RequestBody CreateProductRequest request) {
		log.info("POST /api/products");
		return productService.createProduct(request);
	}

	@PatchMapping("/{id}")
	public ProductDto updateProduct(
			@PathVariable Long id,
			@Valid @RequestBody UpdateProductRequest request) {
		log.info("PATCH /api/products/{}", id);
		return productService.updateProduct(id, request);
	}
}
package ru.yandex.practicum.product.exception;

public class ProductNotFoundException extends NotFoundException {

	public ProductNotFoundException(Long id) {
		super("Product not found with id: " + id);
	}

	public ProductNotFoundException(String message) {
		super(message);
	}
}
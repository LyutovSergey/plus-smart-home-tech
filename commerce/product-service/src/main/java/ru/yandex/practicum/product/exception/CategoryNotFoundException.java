package ru.yandex.practicum.product.exception;

public class CategoryNotFoundException extends NotFoundException {

	public CategoryNotFoundException(Long id) {
		super("Category not found with id: " + id);
	}

	public CategoryNotFoundException(String message) {
		super(message);
	}
}
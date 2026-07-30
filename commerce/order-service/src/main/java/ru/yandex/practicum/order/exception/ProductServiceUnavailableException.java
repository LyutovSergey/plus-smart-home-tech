package ru.yandex.practicum.order.exception;

public class ProductServiceUnavailableException extends RuntimeException {

	public ProductServiceUnavailableException(Long productId, Throwable cause) {
		super("Сервис товаров временно недоступен. Не удалось получить данные для товара id: " + productId, cause);
	}

	public ProductServiceUnavailableException(String message) {
		super(message);
	}
}
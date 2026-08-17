package ru.yandex.practicum.order.exception;

public class InventoryServiceUnavailableException extends RuntimeException {

	public InventoryServiceUnavailableException(Long productId, Throwable cause) {
		super("Сервис склада временно недоступен. Не удалось выполнить операцию для товара id: " + productId, cause);
	}

	public InventoryServiceUnavailableException(String message) {
		super(message);
	}
}
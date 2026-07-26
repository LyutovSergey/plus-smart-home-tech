package ru.yandex.practicum.inventory.exception;

public class InventoryNotFoundException extends NotFoundException {

	public InventoryNotFoundException(Long productId) {
		super("Inventory not found for product id: " + productId);
	}

}
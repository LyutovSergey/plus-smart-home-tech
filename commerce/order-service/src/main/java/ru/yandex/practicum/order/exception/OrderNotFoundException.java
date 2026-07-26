package ru.yandex.practicum.order.exception;

public class OrderNotFoundException extends NotFoundException {

	public OrderNotFoundException(Long id) {
		super("Заказ не найден с id: " + id);
	}


}
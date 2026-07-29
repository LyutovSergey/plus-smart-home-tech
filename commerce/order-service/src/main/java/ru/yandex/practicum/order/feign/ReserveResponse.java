package ru.yandex.practicum.order.feign;

public record ReserveResponse(
		Long productId,
		Integer reservedQuantity,
		Integer availableQuantity
) {
}
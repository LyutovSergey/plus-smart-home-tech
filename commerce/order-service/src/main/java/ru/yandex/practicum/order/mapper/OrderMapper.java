package ru.yandex.practicum.order.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrderMapper {

	public OrderItem toEntity(OrderItemRequest request, Order order) {
		return new OrderItem(
				order,
				request.productId(),
				request.productName(),
				request.quantity(),
				request.price()
		);
	}

	public OrderItemDto toDto(OrderItem item) {
		if (item == null) {
			return null;
		}
		return new OrderItemDto(
				item.getId(),
				item.getProductId(),
				item.getProductName(),
				item.getQuantity(),
				item.getPrice()
		);
	}

	public OrderDto toDto(Order order) {
		if (order == null) {
			return null;
		}

		List<OrderItemDto> itemDtos = order.getItems().stream()
				.map(this::toDto)
				.collect(Collectors.toList());

		return new OrderDto(
				order.getId(),
				order.getCustomerName(),
				order.getCustomerEmail(),
				order.getStatus(),
				order.getTotalPrice(),
				order.getStatusDetails(),
				order.getCreatedAt(),
				itemDtos
		);
	}
}
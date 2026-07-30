package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.OrderNotFoundException;
import ru.yandex.practicum.order.feign.*;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {
	private final ProductClient productClient;
	private final InventoryClient inventoryClient;
	private final OrderRepository orderRepository;
	private final OrderMapper orderMapper;

	public List<OrderDto> getAllOrders() {
		log.info("Запрос всех заказов");
		List<Order> orders = orderRepository.findAll();
		log.debug("Найдено {} заказов", orders.size());
		return orders.stream()
				.map(orderMapper::toDto)
				.collect(Collectors.toList());
	}

	public OrderDto getOrderById(Long id) {
		log.info("Ищем заказ с id: {}", id);
		Order order = orderRepository.findById(id)
				.orElseThrow(() -> {
					log.warn("Заказ с id {} не найден", id);
					return new OrderNotFoundException(id);
				});
		log.debug("Нашли заказ: {}", order.getId());
		return orderMapper.toDto(order);
	}

	public List<OrderDto> getOrdersByEmail(String email) {
		log.info("Ищем заказы по email: {}", email);
		List<Order> orders = orderRepository.findByCustomerEmail(email);
		log.debug("Найдено {} заказов для email {}", orders.size(), email);
		return orders.stream()
				.map(orderMapper::toDto)
				.collect(Collectors.toList());
	}

	@Transactional
	public OrderDto saveOrder(String customerName, String customerEmail, BigDecimal totalPrice, List<OrderItem> items) {
		log.info("Сохраняем заказ для клиента: {}", customerEmail);

		Order order = new Order();
		order.setCustomerName(customerName);
		order.setCustomerEmail(customerEmail);
		order.setTotalPrice(totalPrice);
		order.setStatus("CONFIRMED");
		order.setItems(items);

		for (OrderItem item : items) {
			item.setOrder(order);
		}

		Order saved = orderRepository.save(order);
		log.info("Заказ сохранён с id: {}", saved.getId());

		return orderMapper.toDto(saved);
	}
}
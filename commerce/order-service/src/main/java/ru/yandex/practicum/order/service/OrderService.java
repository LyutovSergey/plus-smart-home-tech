package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.OrderNotFoundException;
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
	public OrderDto createOrder(CreateOrderRequest request) {
		log.info("Создаём заказ для клиента: {}", request.customerEmail());

		// Рассчитываем общую сумму
		BigDecimal totalPrice = request.items().stream()
				.map(item -> item.price().multiply(BigDecimal.valueOf(item.quantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		log.debug("Общая сумма заказа: {}", totalPrice);

		// Создаём заказ
		Order order = new Order();
		order.setCustomerName(request.customerName());
		order.setCustomerEmail(request.customerEmail());
		order.setTotalPrice(totalPrice);
		order.setStatus("CREATED");

		// Создаём позиции
		List<OrderItem> items = request.items().stream()
				.map(itemRequest -> {
					OrderItem item = orderMapper.toEntity(itemRequest, order);
					log.debug("Добавили товар: {} x {} = {}",
							itemRequest.productName(),
							itemRequest.quantity(),
							itemRequest.price().multiply(BigDecimal.valueOf(itemRequest.quantity())));
					return item;
				})
				.collect(Collectors.toList());

		order.setItems(items);

		Order saved = orderRepository.save(order);
		log.info("Заказ создан с id: {}, общая сумма: {}", saved.getId(), saved.getTotalPrice());

		return orderMapper.toDto(saved);
	}
}
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
import ru.yandex.practicum.order.feign.*;
import ru.yandex.practicum.order.mapper.OrderMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
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
	public OrderDto createOrder(CreateOrderRequest request) {
		log.info("Создаём заказ для клиента: {}", request.customerEmail());

		ProductDto product;
		ReserveRequest reserveRequest;

		List<OrderItem> items = new ArrayList<>();
		BigDecimal totalPrice = BigDecimal.ZERO;

		Order order = new Order();

		for (OrderItemRequest orderItemRequest : request.items()) {

			product = productClient.getProductById(orderItemRequest.productId());

			if (!product.active()) {
				throw new IllegalStateException("Товар " + product.name() + " снят с продажи");
			}

			BigDecimal itemPrice = product.price();
			BigDecimal itemTotal = itemPrice.multiply(BigDecimal.valueOf(orderItemRequest.quantity()));
			totalPrice = totalPrice.add(itemTotal);

			// Актуализация позиции товара
			OrderItem item = new OrderItem();
			item.setProductId(product.id());
			item.setProductName(product.name());  // актуальное название
			item.setQuantity(orderItemRequest.quantity());
			item.setPrice(itemPrice);             // актуальная цена
			item.setOrder(order);
			items.add(item);

			log.debug("Добавили товар: {} x {} = {}",
					item.getProductName(),
					item.getQuantity(),
					item.getPrice());
		}

		//резервируем товар
		for (OrderItem item:items){

			reserveRequest = new ReserveRequest(
					item.getProductId(),
					item.getQuantity()
			);

			try {
				ReserveResponse reserveResponse = inventoryClient.reserveStock(reserveRequest);
				log.debug("Зарезервировано: {} шт товара {}, осталось доступно: {}",
						item.getQuantity(), item.getProductId(), reserveResponse.availableQuantity());
			} catch (Exception e) {
				log.error("Ошибка резервирования товара {}: {}", item.getProductId(), e.getMessage());
				throw new RuntimeException("Не удалось зарезервировать товар: " + e.getMessage(), e);
			}
		}

		log.debug("Общая сумма заказа: {}", totalPrice);

		// Создаём заказ
		order.setCustomerName(request.customerName());
		order.setCustomerEmail(request.customerEmail());
		order.setTotalPrice(totalPrice);
		order.setStatus("CREATED");
		order.setItems(items);
		Order saved = orderRepository.save(order);
		log.info("Заказ создан с id: {}, общая сумма: {}", saved.getId(), saved.getTotalPrice());
		return orderMapper.toDto(saved);
	}
}
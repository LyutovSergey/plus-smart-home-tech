package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.*;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderOrchestrationService {

	private final ProductClient productClient;
	private final InventoryClient inventoryClient;
	private final OrderService orderService;

	public OrderDto createOrder(CreateOrderRequest request) {
		log.info("Оркестрация заказа для клиента: {}", request.customerEmail());

		//Группируем товары по productId
		Map<Long, Integer> productQuantities = request.items().stream()
				.collect(Collectors.groupingBy(
						OrderItemRequest::productId,
						Collectors.summingInt(OrderItemRequest::quantity)
				));

		log.debug("Сгруппированные товары: {}", productQuantities);

		//Кеш товаров заказа
		Map<Long, ProductDto> productCache = new HashMap<>();
		Map<Long, Integer> reservedProducts = new HashMap<>();

		List<OrderItem> orderItems = new ArrayList<>();
		BigDecimal totalPrice = BigDecimal.ZERO;

		try {
			for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
				Long productId = entry.getKey();
				Integer totalQuantity = entry.getValue();

				ProductDto product;
				try {
					product = productCache.computeIfAbsent(productId,
							id -> productClient.getProductById(id));
				} catch (FeignException e) {
					if (e.status() == 404) {
						throw new OrderProcessingException("Товар с id " + productId + " не найден в каталоге");
					}
					throw new OrderProcessingException("Ошибка при получении данных товара", e);
				}

				if (!product.active()) {
					throw new OrderProcessingException("Товар '" + product.name() + "' снят с продажи");
				}

				ReserveRequest reserveRequest = new ReserveRequest(productId, totalQuantity);
				ReserveResponse reserveResponse;

				try {
					reserveResponse = inventoryClient.reserveStock(reserveRequest);
				} catch (FeignException e) {
					if (e.status() == 404) {
						throw new OrderProcessingException("Складская запись для товара '" + product.name() + "' не найдена");
					}
					if (e.status() == 409) {
						throw new OrderProcessingException("Недостаточно товара '" + product.name() + "' на складе");
					}
					throw new OrderProcessingException("Ошибка при резервировании товара", e);
				}

				if (!reserveResponse.success()) {
					throw new OrderProcessingException(reserveResponse.message());
				}

				// Запоминаем для отката
				reservedProducts.put(productId, totalQuantity);

				// Создаём позицию заказа
				OrderItem item = new OrderItem();
				item.setProductId(product.id());
				item.setProductName(product.name());
				item.setQuantity(totalQuantity);
				item.setPrice(product.price());
				orderItems.add(item);

				BigDecimal itemTotal = product.price().multiply(BigDecimal.valueOf(totalQuantity));
				totalPrice = totalPrice.add(itemTotal);

				log.debug("Товар: {} x {} = {}", product.name(), totalQuantity, itemTotal);
			}

			// Сохраняем заказ
			return orderService.saveOrder(
					request.customerName(),
					request.customerEmail(),
					totalPrice,
					orderItems
			);

		} catch (OrderProcessingException e) {
			// Откат резерва
			compensate(reservedProducts);
			throw e;

		} catch (Exception e) {
			log.error("Неожиданная ошибка при создании заказа", e);

			compensate(reservedProducts);

			// Если это FeignException, который не обработали выше
			if (e instanceof FeignException feignEx) {
				if (feignEx.status() == 404) {
					throw new OrderProcessingException("Товар не найден в каталоге");
				}
				if (feignEx.status() == 409) {
					throw new OrderProcessingException("Недостаточно товара на складе");
				}
				throw new OrderProcessingException("Ошибка при обращении к соседнему сервису");
			}

			throw new OrderProcessingException("Не удалось создать заказ. Попробуйте позже.", e);
		}
	}

	//Снятие с резерва
	private void compensate(Map<Long, Integer> reservedProducts) {
		if (reservedProducts.isEmpty()) {
			return;
		}

		log.info("Выполняем компенсацию для {} товаров", reservedProducts.size());

		for (Map.Entry<Long, Integer> entry : reservedProducts.entrySet()) {
			try {
				ReserveRequest releaseRequest = new ReserveRequest(entry.getKey(), entry.getValue());
				inventoryClient.releaseStock(releaseRequest);
				log.debug("Снят резерв для товара {}: {}", entry.getKey(), entry.getValue());
			} catch (Exception ex) {
				log.error("НЕ УДАЛОСЬ снять резерв для товара {}: {}", entry.getKey(), ex.getMessage());
				// Не бросаем исключение, чтобы не ломать основной ответ
			}
		}
	}
}
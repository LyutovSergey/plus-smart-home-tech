package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.dto.ProductDto;
import ru.yandex.practicum.order.client.dto.ReserveRequest;
import ru.yandex.practicum.order.client.dto.ReserveResponse;
import ru.yandex.practicum.order.client.dto.ServiceCallResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.entity.OrderStatus;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

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

		// Группируем товары по productId
		Map<Long, Integer> productQuantities = request.items().stream()
				.collect(Collectors.groupingBy(
						OrderItemRequest::productId,
						Collectors.summingInt(OrderItemRequest::quantity)
				));

		log.debug("Сгруппированные товары: {}", productQuantities);

		// Кеш товаров заказа
		Map<Long, ProductDto> productCache = new HashMap<>();
		Map<Long, Integer> reservedProducts = new HashMap<>();

		List<OrderItem> orderItems = new ArrayList<>();
		BigDecimal totalPrice = BigDecimal.ZERO;
		boolean isDegraded = false;
		String degradationReason = null;

		try {
			for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
				Long productId = entry.getKey();
				Integer totalQuantity = entry.getValue();

				// Получаем товар с обработкой деградации
				ServiceCallResult<ProductDto> productResult = getProduct(productId, productCache);

				if (productResult.isBusinessError()) {
					compensate(reservedProducts);
					throw new OrderProcessingException(productResult.getErrorMessage());
				}

				if (!productResult.isSuccess()) {
					isDegraded = true;
					degradationReason = "product-service недоступен";
					OrderItem degradedItem = createDegradedItem(productId, totalQuantity);
					orderItems.add(degradedItem);
					log.warn("Деградация: товар {} сохранён как заглушка", productId);
					continue;
				}

				ProductDto product = productResult.getData();

				if (!product.active()) {
					compensate(reservedProducts);
					throw new OrderProcessingException("Товар '" + product.name() + "' снят с продажи");
				}

				// Резервируем с обработкой деградации
				ServiceCallResult<ReserveResponse> reserveResult = reserveStock(productId, totalQuantity, product);

				if (reserveResult.isBusinessError()) {
					compensate(reservedProducts);
					throw new OrderProcessingException(reserveResult.getErrorMessage());
				}

				if (!reserveResult.isSuccess()) {
					// Деградация: inventory-service недоступен
					isDegraded = true;
					degradationReason = "inventory-service недоступен";
					compensate(reservedProducts);
					OrderItem degradedItem = createDegradedItem(productId, totalQuantity);
					orderItems.add(degradedItem);
					log.warn("Деградация: товар {} сохранён как заглушка (склад недоступен)", productId);
					continue;
				}

				// Успешное резервирование
				reservedProducts.put(productId, totalQuantity);

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

			// Определяем статус заказа
			OrderStatus status = isDegraded ? OrderStatus.PENDING_CONFIRMATION : OrderStatus.CONFIRMED;
			String statusDetails = isDegraded ? degradationReason : null;

			// Сохраняем заказ
			return orderService.saveOrder(
					request.customerName(),
					request.customerEmail(),
					totalPrice,
					orderItems,
					status,
					statusDetails
			);

		} catch (OrderProcessingException e) {
			compensate(reservedProducts);
			throw e;

		} catch (Exception e) {
			log.error("Неожиданная ошибка при создании заказа", e);
			compensate(reservedProducts);

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

	private ServiceCallResult<ProductDto> getProduct(Long productId, Map<Long, ProductDto> productCache) {
		try {
			ProductDto product = productCache.computeIfAbsent(productId,
					id -> productClient.getProductById(id));

			if (product == null) {
				return ServiceCallResult.businessError("Товар с id " + productId + " не найден в каталоге");
			}
			return ServiceCallResult.success(product);
		} catch (ProductServiceUnavailableException e) {
			log.warn("Техническая ошибка при получении товара {}: {}", productId, e.getMessage());
			return ServiceCallResult.technicalError("product-service недоступен");
		} catch (FeignException e) {
			if (e.status() == 404) {
				return ServiceCallResult.businessError("Товар с id " + productId + " не найден в каталоге");
			}
			log.warn("Техническая ошибка при получении товара {}: {}", productId, e.getMessage());
			return ServiceCallResult.technicalError("product-service недоступен");
		} catch (Exception e) {
			log.warn("Техническая ошибка при получении товара {}: {}", productId, e.getMessage());
			return ServiceCallResult.technicalError("product-service недоступен");
		}
	}

	private ServiceCallResult<ReserveResponse> reserveStock(Long productId, Integer quantity, ProductDto product) {
		try {
			ReserveRequest reserveRequest = new ReserveRequest(productId, quantity);
			ReserveResponse response = inventoryClient.reserveStock(reserveRequest);

			if (!response.success()) {
				return ServiceCallResult.businessError(response.message());
			}
			return ServiceCallResult.success(response);
		} catch (FeignException e) {
			if (e.status() == 409) {
				return ServiceCallResult.businessError("Недостаточно товара '" + product.name() + "' на складе");
			}
			if (e.status() == 404) {
				return ServiceCallResult.businessError("Складская запись для товара '" + product.name() + "' не найдена");
			}
			log.warn("Техническая ошибка при резервировании товара {}: {}", productId, e.getMessage());
			return ServiceCallResult.technicalError("inventory-service недоступен");
		} catch (InventoryServiceUnavailableException e) {
			log.warn("Техническая ошибка при резервировании товара {}: {}", productId, e.getMessage());
			return ServiceCallResult.technicalError("inventory-service недоступен");
		} catch (Exception e) {
			log.warn("Техническая ошибка при резервировании товара {}: {}", productId, e.getMessage());
			return ServiceCallResult.technicalError("inventory-service недоступен");
		}
	}

	private OrderItem createDegradedItem(Long productId, Integer quantity) {
		OrderItem item = new OrderItem();
		item.setProductId(productId);
		item.setProductName("Товар #" + productId + " (ожидает проверки)");
		item.setQuantity(quantity);
		item.setPrice(BigDecimal.ZERO);
		return item;
	}

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
			}
		}
	}
}
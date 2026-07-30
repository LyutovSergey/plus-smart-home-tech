package ru.yandex.practicum.order.client;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.dto.ProductDto;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;

@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

	private static final Logger log = LoggerFactory.getLogger(ProductClientFallbackFactory.class);

	@Override
	public ProductClient create(Throwable cause) {
		return productId -> {
			// 404 — бизнес-отказ, возвращаем null
			if (cause instanceof FeignException feignEx && feignEx.status() == 404) {
				log.warn("Товар с id {} не найден в каталоге (404)", productId);
				return null;
			}

			// Техническая ошибка — бросаем исключение
			log.warn("product-service недоступен при запросе товара id={}", productId, cause);
			throw new ProductServiceUnavailableException(productId, cause);
		};
	}
}
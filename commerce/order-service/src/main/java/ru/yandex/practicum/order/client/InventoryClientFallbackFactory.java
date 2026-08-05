package ru.yandex.practicum.order.client;

import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.client.dto.ReserveRequest;
import ru.yandex.practicum.order.client.dto.ReserveResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;

@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

	private static final Logger log = LoggerFactory.getLogger(InventoryClientFallbackFactory.class);

	@Override
	public InventoryClient create(Throwable cause) {
		return new InventoryClient() {

			@Override
			public ReserveResponse reserveStock(ReserveRequest request) {
				if (cause instanceof FeignException feignEx && feignEx.status() == 409) {
					log.warn("Недостаточно товара на складе: productId={}", request.productId());
					return new ReserveResponse(false, null, "Недостаточно товара на складе");
				}

				log.warn("inventory-service недоступен при резервировании товара id={}", request.productId(), cause);
				throw new InventoryServiceUnavailableException(request.productId(), cause);
			}

			@Override
			public ReserveResponse releaseStock(ReserveRequest request) {
				log.warn("inventory-service недоступен при снятии резерва товара id={}", request.productId(), cause);
				throw new InventoryServiceUnavailableException(request.productId(), cause);
			}
		};
	}
}
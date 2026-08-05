package ru.yandex.practicum.order.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import ru.yandex.practicum.order.client.InventoryClient;
import ru.yandex.practicum.order.client.ProductClient;
import ru.yandex.practicum.order.client.dto.ProductDto;
import ru.yandex.practicum.order.client.dto.ReserveResponse;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@TestConfiguration
public class TestConfig {

	@Bean
	public ProductClient productClient() {
		ProductClient mock = mock(ProductClient.class);

		when(mock.getProductById(1L)).thenReturn(
				new ProductDto(1L, "Acceptance Smart Lamp", "Test lamp", new BigDecimal("3490.00"), true)
		);
		when(mock.getProductById(2L)).thenReturn(
				new ProductDto(2L, "Acceptance Smart Plug", "Test plug", new BigDecimal("1290.00"), true)
		);

		return mock;
	}

	@Bean
	public InventoryClient inventoryClient() {
		InventoryClient mock = mock(InventoryClient.class);

		when(mock.reserveStock(any())).thenReturn(
				new ReserveResponse(true, 100, "Stock reserved")
		);
		when(mock.releaseStock(any())).thenReturn(
				new ReserveResponse(true, 100, "Stock released")
		);

		return mock;
	}
}
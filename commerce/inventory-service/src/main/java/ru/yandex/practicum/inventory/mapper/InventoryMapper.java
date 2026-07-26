package ru.yandex.practicum.inventory.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;

@Component
public class InventoryMapper {

	public Inventory toEntity(UpdateInventoryRequest request) {
		return new Inventory(request.productId(), request.quantity());
	}

	public InventoryDto toDto(Inventory inventory) {
		if (inventory == null) {
			return null;
		}
		return new InventoryDto(
				inventory.getId(),
				inventory.getProductId(),
				inventory.getQuantity(),
				inventory.getReservedQuantity(),
				inventory.getAvailableQuantity()
		);
	}
}
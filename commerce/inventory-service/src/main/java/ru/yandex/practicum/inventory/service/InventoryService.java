package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.Inventory;
import ru.yandex.practicum.inventory.exception.DuplicateInventoryException;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.InventoryNotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

	private final InventoryRepository inventoryRepository;
	private final InventoryMapper inventoryMapper;

	public List<InventoryDto> getAllInventory() {
		log.info("Запрос всех остатков на складе");
		List<Inventory> all = inventoryRepository.findAll();
		log.debug("Найдено {} записей", all.size());
		return all.stream()
				.map(inventoryMapper::toDto)
				.collect(Collectors.toList());
	}

	public InventoryDto getByProductId(Long productId) {
		log.info("Ищем остатки для товара с id: {}", productId);
		Inventory inventory = inventoryRepository.findByProductId(productId)
				.orElseThrow(() -> {
					log.warn("Остатки для товара {} не найдены", productId);
					return new InventoryNotFoundException(productId);
				});
		log.debug("Нашли: количество={}, зарезервировано={}, доступно={}",
				inventory.getQuantity(),
				inventory.getReservedQuantity(),
				inventory.getAvailableQuantity());
		return inventoryMapper.toDto(inventory);
	}

	@Transactional
	public InventoryDto createInventory(UpdateInventoryRequest request) {
		log.info("Создаём остатки для товара: {}", request.productId());

		if (inventoryRepository.existsByProductId(request.productId())) {
			log.warn("Остатки для товара {} уже существуют", request.productId());
			throw new DuplicateInventoryException(request.productId());
		}

		Inventory inventory = inventoryMapper.toEntity(request);
		Inventory saved = inventoryRepository.save(inventory);
		log.info("Остатки для товара {} созданы, id записи: {}", request.productId(), saved.getId());
		return inventoryMapper.toDto(saved);
	}

	@Transactional
	public InventoryDto updateInventory(UpdateInventoryRequest request) {
		log.info("Обновляем остатки для товара: {}", request.productId());

		Inventory inventory = inventoryRepository.findByProductId(request.productId())
				.orElseThrow(() -> {
					log.warn("Остатки для товара {} не найдены", request.productId());
					return new InventoryNotFoundException(request.productId());
				});

		int oldQuantity = inventory.getQuantity();
		inventory.setQuantity(request.quantity());
		Inventory updated = inventoryRepository.save(inventory);

		log.info("Количество для товара {} обновлено: {} -> {}",
				request.productId(), oldQuantity, request.quantity());
		return inventoryMapper.toDto(updated);
	}

	@Transactional
	public ReserveResponse reserve(ReserveRequest request) {
		log.info("Пытаемся зарезервировать {} единиц товара {}", request.quantity(), request.productId());

		try {
			Inventory inventory = inventoryRepository.findByProductId(request.productId())
					.orElseThrow(() -> {
						log.warn("Остатки для товара {} не найдены", request.productId());
						return new InventoryNotFoundException(request.productId());
					});

			int available = inventory.getAvailableQuantity();
			log.debug("Доступно для товара {}: {}", request.productId(), available);

			if (available < request.quantity()) {
				log.warn("Не хватает товара {}: доступно {}, запрошено {}",
						request.productId(), available, request.quantity());
				throw new InsufficientStockException(
						"Недостаточно товара. Доступно: " + available + ", запрошено: " + request.quantity()
				);
			}

			inventory.setReservedQuantity(inventory.getReservedQuantity() + request.quantity());
			Inventory saved = inventoryRepository.save(inventory);

			log.info("Успешно зарезервировали {} единиц товара {}. Осталось доступно: {}",
					request.quantity(), request.productId(), saved.getAvailableQuantity());

			return new ReserveResponse(
					true,
					saved.getAvailableQuantity(),
					"Товар успешно зарезервирован"
			);

		} catch (ObjectOptimisticLockingFailureException e) {
			log.warn("Кто-то параллельно изменил остатки товара {}, повторите запрос", request.productId());
			throw new ObjectOptimisticLockingFailureException(
					Inventory.class,
					"Конфликт конкурентного доступа. Данные были изменены другим запросом. Повторите операцию."
			);
		}
	}
}
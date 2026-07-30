package ru.yandex.practicum.inventory.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.service.InventoryService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

	private final InventoryService inventoryService;

	@GetMapping
	public List<InventoryDto> getAllInventory() {
		log.info("GET /api/inventory - запрос всех остатков");
		return inventoryService.getAllInventory();
	}

	@GetMapping("/{productId}")
	public InventoryDto getByProductId(@PathVariable Long productId) {
		log.info("GET /api/inventory/{} - запрос остатков по товару", productId);
		return inventoryService.getByProductId(productId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public InventoryDto createInventory(@Valid @RequestBody UpdateInventoryRequest request) {
		log.info("POST /api/inventory - создание остатков для товара {}", request.productId());
		return inventoryService.createInventory(request);
	}

	@PutMapping
	public InventoryDto updateInventory(@Valid @RequestBody UpdateInventoryRequest request) {
		log.info("PUT /api/inventory - обновление остатков для товара {}", request.productId());
		return inventoryService.updateInventory(request);
	}

	@PostMapping("/reserve")
	public ReserveResponse reserve(@Valid @RequestBody ReserveRequest request) {
		log.info("POST /api/inventory/reserve - резервирование {} единиц товара {}",
				request.quantity(), request.productId());
		return inventoryService.reserve(request);
	}

	@PostMapping("/release")
	public ReserveResponse release(@Valid @RequestBody ReserveRequest request) {
		log.info("POST /api/inventory/release - снятие с резерва {} единиц товара {}",
				request.quantity(), request.productId());
		return inventoryService.release(request);

	}
}
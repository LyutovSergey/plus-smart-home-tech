package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.service.OrderService;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

	private final OrderService orderService;

	@GetMapping
	public List<OrderDto> getAllOrders() {
		log.info("GET /api/orders - запрос всех заказов");
		return orderService.getAllOrders();
	}

	@GetMapping("/{id}")
	public OrderDto getOrderById(@PathVariable Long id) {
		log.info("GET /api/orders/{} - запрос заказа по id", id);
		return orderService.getOrderById(id);
	}

	@GetMapping("/by-email")
	public List<OrderDto> getOrdersByEmail(@RequestParam String email) {
		log.info("GET /api/orders/by-email?email={} - поиск заказов по email", email);
		return orderService.getOrdersByEmail(email);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public OrderDto createOrder(@Valid @RequestBody CreateOrderRequest request) {
		log.info("POST /api/orders - создание заказа для клиента: {}", request.customerEmail());
		return orderService.createOrder(request);
	}
}
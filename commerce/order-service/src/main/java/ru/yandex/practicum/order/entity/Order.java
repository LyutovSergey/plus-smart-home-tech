package ru.yandex.practicum.order.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "customer_name", nullable = false)
	private String customerName;

	@Column(name = "customer_email", nullable = false)
	private String customerEmail;

	@Column(nullable = false)
	private String status = "CREATED";

	@Column(name = "total_price", nullable = false)
	private BigDecimal totalPrice;

	@Column(name = "status_details")
	private String statusDetails;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private List<OrderItem> items = new ArrayList<>();

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
		if (status == null) {
			status = "CREATED";
		}
	}

	public Order(String customerName, String customerEmail, BigDecimal totalPrice, List<OrderItem> items) {
		this.customerName = customerName;
		this.customerEmail = customerEmail;
		this.totalPrice = totalPrice;
		this.items = items;
		this.status = "CREATED";
		this.createdAt = LocalDateTime.now();
	}
}
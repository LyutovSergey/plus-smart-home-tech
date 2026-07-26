package ru.yandex.practicum.inventory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inventory")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "product_id", nullable = false, unique = true)
	private Long productId;

	@Column(nullable = false)
	private Integer quantity = 0;

	@Column(name = "reserved_quantity", nullable = false)
	private Integer reservedQuantity = 0;

	@Version
	private Long version;

	@Transient
	public Integer getAvailableQuantity() {
		return quantity - reservedQuantity;
	}

	public Inventory(Long productId, Integer quantity) {
		this.productId = productId;
		this.quantity = quantity;
		this.reservedQuantity = 0;
	}
}
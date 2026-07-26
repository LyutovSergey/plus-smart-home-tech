package ru.yandex.practicum.product.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(length = 2000)
	private String description;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(nullable = false)
	private Boolean active = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private Category category;

	public Product(String name, String description, BigDecimal price, String imageUrl, Category category) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.imageUrl = imageUrl;
		this.category = category;
		this.active = true;
	}
}
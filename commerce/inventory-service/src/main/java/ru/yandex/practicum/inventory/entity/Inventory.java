package ru.yandex.practicum.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import java.util.Objects;

@Entity
@Table(name = "inventory")
@Getter
@Setter
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
	@Override
	public final boolean equals(Object o) {
		if (this == o) return true;
		if (o == null) return false;

		Class<?> oEffectiveClass = o instanceof HibernateProxy
				? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass()
				: o.getClass();
		Class<?> thisEffectiveClass = this instanceof HibernateProxy
				? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass()
				: this.getClass();

		if (thisEffectiveClass != oEffectiveClass) return false;

		Inventory inventory = (Inventory) o;
		return getId() != null && Objects.equals(getId(), inventory.getId());
	}

	@Override
	public final int hashCode() {
		return this instanceof HibernateProxy
				? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode()
				: getClass().hashCode();
	}
}
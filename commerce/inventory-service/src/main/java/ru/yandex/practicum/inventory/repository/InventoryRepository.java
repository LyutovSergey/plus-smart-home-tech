package ru.yandex.practicum.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.inventory.entity.Inventory;

import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

	Optional<Inventory> findByProductId(Long productId);

	boolean existsByProductId(Long productId);
}
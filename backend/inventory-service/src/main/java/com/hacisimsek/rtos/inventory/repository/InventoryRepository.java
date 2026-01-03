package com.hacisimsek.rtos.inventory.repository;

import com.hacisimsek.rtos.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {}

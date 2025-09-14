package com.hacisimsek.inventory.repository;

import com.hacisimsek.inventory.domain.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<InventoryItem, String> {}

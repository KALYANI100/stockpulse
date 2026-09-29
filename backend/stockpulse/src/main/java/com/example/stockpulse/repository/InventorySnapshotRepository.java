package com.example.stockpulse.repository;

import com.example.stockpulse.domain.InventorySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InventorySnapshotRepository extends JpaRepository<InventorySnapshot, UUID> {
}

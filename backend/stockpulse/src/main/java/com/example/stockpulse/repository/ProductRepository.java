package com.example.stockpulse.repository;

import com.example.stockpulse.domain.Category;
import com.example.stockpulse.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySku(String sku);
    List<Product> findAllByOrderByNameAsc();
    List<Product> findByCategory(Category category);
}

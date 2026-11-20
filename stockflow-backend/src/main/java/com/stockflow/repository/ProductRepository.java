package com.stockflow.repository;

import com.stockflow.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<Product> findAllByActiveTrue();

    Optional<Product> findByIdAndActiveTrue(Long id);
}
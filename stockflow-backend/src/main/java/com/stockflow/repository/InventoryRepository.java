
package com.stockflow.repository;

import com.stockflow.entity.Inventory;
import com.stockflow.entity.Product;
import com.stockflow.entity.Warehouse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductAndWarehouse(
            Product product, Warehouse warehouse);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM Inventory i
            WHERE i.product.id = :productId
            AND i.warehouse.id = :warehouseId
            """)
    Optional<Inventory> findByProductAndWarehouseForUpdate(
            @Param("productId") Long productId,
            @Param("warehouseId") Long warehouseId);

    List<Inventory> findByProductId(Long productId);

    List<Inventory> findByWarehouseId(Long warehouseId);
}

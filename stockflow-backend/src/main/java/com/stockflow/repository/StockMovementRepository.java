
package com.stockflow.repository;

import com.stockflow.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository
        extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByProductId(
            Long productId, Pageable pageable);

    Page<StockMovement> findByWarehouseId(
            Long warehouseId, Pageable pageable);

    Page<StockMovement> findByMovementType(
            String movementType, Pageable pageable);
}

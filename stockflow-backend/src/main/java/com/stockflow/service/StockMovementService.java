
package com.stockflow.service;

import java.math.BigDecimal;
import java.util.List;

import com.stockflow.dto.StockInRequest;
import com.stockflow.dto.StockInResponse;
import com.stockflow.dto.StockOutRequest;
import com.stockflow.dto.StockOutResponse;
import com.stockflow.dto.StockMovementResponse;
import com.stockflow.dto.StockTransferRequest;
import com.stockflow.dto.StockTransferResponse;

import com.stockflow.entity.Inventory;
import com.stockflow.entity.Product;
import com.stockflow.entity.StockMovement;
import com.stockflow.entity.Warehouse;

import com.stockflow.repository.InventoryRepository;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.StockMovementRepository;
import com.stockflow.repository.WarehouseRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockMovementService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final StockMovementRepository stockMovementRepository;

    public StockMovementService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            StockMovementRepository stockMovementRepository) {

        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    // =========================
    // GET ACTIVE PRODUCT
    // =========================

    private Product getActiveProduct(Long productId) {

        return productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Product not found or inactive"));
    }

    // =========================
    // STOCK IN
    // =========================

    @Transactional
    public StockInResponse stockIn(StockInRequest request) {

        Product product = getActiveProduct(request.getProductId());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Warehouse not found"));

        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new IllegalArgumentException("Warehouse is inactive");
        }

        Inventory inventory =
                inventoryRepository.findByProductAndWarehouseForUpdate(
                        product.getId(), warehouse.getId())
                        .orElseGet(() ->
                                new Inventory(product, warehouse, BigDecimal.ZERO));

        inventory.setQuantity(
                inventory.getQuantity().add(request.getQuantity()));

        inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setMovementType("STOCK_IN");
        movement.setQuantity(request.getQuantity());
        movement.setReference(request.getReference());
        movement.setRemarks(request.getRemarks());

        StockMovement savedMovement =
                stockMovementRepository.save(movement);

        return new StockInResponse(
                savedMovement.getId(),
                product.getId(),
                product.getName(),
                warehouse.getId(),
                warehouse.getName(),
                savedMovement.getMovementType(),
                savedMovement.getQuantity(),
                inventory.getQuantity(),
                savedMovement.getReference(),
                savedMovement.getRemarks(),
                savedMovement.getCreatedAt()
        );
    }

    // =========================
    // STOCK OUT
    // =========================

    @Transactional
    public StockOutResponse stockOut(StockOutRequest request) {

        Product product = getActiveProduct(request.getProductId());

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() ->
                        new IllegalArgumentException("Warehouse not found"));

        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new IllegalArgumentException("Warehouse is inactive");
        }

        Inventory inventory =
                inventoryRepository.findByProductAndWarehouseForUpdate(
                        product.getId(), warehouse.getId())
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Inventory record not found for this product and warehouse"));

        if (inventory.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock. Available quantity: "
                            + inventory.getQuantity());
        }

        inventory.setQuantity(
                inventory.getQuantity().subtract(request.getQuantity()));

        inventoryRepository.save(inventory);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse);
        movement.setMovementType("STOCK_OUT");
        movement.setQuantity(request.getQuantity());
        movement.setReference(request.getReference());
        movement.setRemarks(request.getRemarks());

        StockMovement savedMovement =
                stockMovementRepository.save(movement);

        return new StockOutResponse(
                savedMovement.getId(),
                product.getId(),
                product.getName(),
                warehouse.getId(),
                warehouse.getName(),
                savedMovement.getMovementType(),
                savedMovement.getQuantity(),
                inventory.getQuantity(),
                savedMovement.getReference(),
                savedMovement.getRemarks(),
                savedMovement.getCreatedAt()
        );
    }

    // =========================
    // STOCK TRANSFER
    // =========================

    @Transactional
    public StockTransferResponse transferStock(StockTransferRequest request) {

        Long productId = request.getProductId();
        Long sourceWarehouseId = request.getSourceWarehouseId();
        Long destinationWarehouseId = request.getDestinationWarehouseId();

        if (sourceWarehouseId.equals(destinationWarehouseId)) {
            throw new IllegalArgumentException(
                    "Source and destination warehouses must be different");
        }

        Product product = getActiveProduct(productId);

        Warehouse sourceWarehouse =
                warehouseRepository.findById(sourceWarehouseId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Source warehouse not found"));

        Warehouse destinationWarehouse =
                warehouseRepository.findById(destinationWarehouseId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Destination warehouse not found"));

        if (!Boolean.TRUE.equals(sourceWarehouse.getActive())) {
            throw new IllegalArgumentException(
                    "Source warehouse is inactive");
        }

        if (!Boolean.TRUE.equals(destinationWarehouse.getActive())) {
            throw new IllegalArgumentException(
                    "Destination warehouse is inactive");
        }

        /*
         * Retrieve inventory rows in ascending warehouse ID order.
         * This keeps the existing lock-order strategy consistent.
         *
         * The source inventory must exist.
         * The destination inventory is allowed to be absent.
         */

        Long firstWarehouseId =
                Math.min(sourceWarehouseId, destinationWarehouseId);

        Long secondWarehouseId =
                Math.max(sourceWarehouseId, destinationWarehouseId);

        Inventory firstInventory =
                inventoryRepository.findByProductAndWarehouseForUpdate(
                        productId, firstWarehouseId)
                        .orElse(null);

        Inventory secondInventory =
                inventoryRepository.findByProductAndWarehouseForUpdate(
                        productId, secondWarehouseId)
                        .orElse(null);

        Inventory sourceInventory =
                sourceWarehouseId.equals(firstWarehouseId)
                        ? firstInventory
                        : secondInventory;

        Inventory destinationInventory =
                destinationWarehouseId.equals(firstWarehouseId)
                        ? firstInventory
                        : secondInventory;

        if (sourceInventory == null) {
            throw new IllegalArgumentException(
                    "Inventory record not found for source warehouse "
                            + sourceWarehouseId);
        }

        // Create destination inventory if it does not exist.
        if (destinationInventory == null) {
            destinationInventory = new Inventory(
                    product,
                    destinationWarehouse,
                    BigDecimal.ZERO
            );
        }

        BigDecimal quantity = request.getQuantity();

        if (sourceInventory.getQuantity().compareTo(quantity) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient stock in source warehouse. Available quantity: "
                            + sourceInventory.getQuantity());
        }

        // Update source and destination inventory.
        sourceInventory.setQuantity(
                sourceInventory.getQuantity().subtract(quantity));

        destinationInventory.setQuantity(
                destinationInventory.getQuantity().add(quantity));

        inventoryRepository.save(sourceInventory);
        inventoryRepository.save(destinationInventory);

        // Create TRANSFER_OUT movement.
        StockMovement transferOut = new StockMovement();
        transferOut.setProduct(product);
        transferOut.setWarehouse(sourceWarehouse);
        transferOut.setMovementType("TRANSFER_OUT");
        transferOut.setQuantity(quantity);
        transferOut.setReference(request.getReference());
        transferOut.setRemarks(request.getRemarks());

        // Create TRANSFER_IN movement.
        StockMovement transferIn = new StockMovement();
        transferIn.setProduct(product);
        transferIn.setWarehouse(destinationWarehouse);
        transferIn.setMovementType("TRANSFER_IN");
        transferIn.setQuantity(quantity);
        transferIn.setReference(request.getReference());
        transferIn.setRemarks(request.getRemarks());

        StockMovement savedTransferOut =
                stockMovementRepository.save(transferOut);

        StockMovement savedTransferIn =
                stockMovementRepository.save(transferIn);

        return new StockTransferResponse(
                savedTransferOut.getId(),
                savedTransferIn.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                sourceWarehouse.getId(),
                sourceWarehouse.getName(),
                destinationWarehouse.getId(),
                destinationWarehouse.getName(),
                quantity,
                sourceInventory.getQuantity(),
                destinationInventory.getQuantity(),
                request.getReference(),
                request.getRemarks(),
                savedTransferOut.getCreatedAt()
        );
    }

    // =========================
    // MAP STOCK MOVEMENT RESPONSE
    // =========================

    private StockMovementResponse mapToResponse(StockMovement movement) {

        return new StockMovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getSku(),
                movement.getWarehouse().getId(),
                movement.getWarehouse().getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getReference(),
                movement.getRemarks(),
                movement.getCreatedAt()
        );
    }

    // =========================
    // GET ALL STOCK MOVEMENTS
    // =========================

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getAllMovements(Pageable pageable) {

        return stockMovementRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    // =========================
    // GET STOCK MOVEMENT BY ID
    // =========================

    @Transactional(readOnly = true)
    public StockMovementResponse getMovementById(Long id) {

        StockMovement movement = stockMovementRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Stock movement not found"));

        return mapToResponse(movement);
    }

    // =========================
    // GET MOVEMENTS BY PRODUCT
    // =========================

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByProduct(
            Long productId, Pageable pageable) {

        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Product not found");
        }

        return stockMovementRepository.findByProductId(productId, pageable)
                .map(this::mapToResponse);
    }

    // =========================
    // GET MOVEMENTS BY WAREHOUSE
    // =========================

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByWarehouse(
            Long warehouseId, Pageable pageable) {

        if (!warehouseRepository.existsById(warehouseId)) {
            throw new IllegalArgumentException("Warehouse not found");
        }

        return stockMovementRepository.findByWarehouseId(
                warehouseId, pageable)
                .map(this::mapToResponse);
    }

    // =========================
    // GET MOVEMENTS BY TYPE
    // =========================

    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByType(
            String type, Pageable pageable) {

        String movementType = type.toUpperCase();

        if (!List.of(
                "STOCK_IN",
                "STOCK_OUT",
                "TRANSFER_IN",
                "TRANSFER_OUT",
                "ADJUSTMENT"
        ).contains(movementType)) {

            throw new IllegalArgumentException("Invalid movement type");
        }

        return stockMovementRepository.findByMovementType(
                movementType, pageable)
                .map(this::mapToResponse);
    }
}

package com.stockflow.service;

import com.stockflow.dto.InventoryRequest;
import com.stockflow.dto.InventoryResponse;
import com.stockflow.entity.Inventory;
import com.stockflow.entity.Product;
import com.stockflow.entity.Warehouse;
import com.stockflow.repository.InventoryRepository;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.WarehouseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository) {

        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
    }

    // Convert Inventory entity into InventoryResponse DTO
    private InventoryResponse mapToResponse(Inventory inventory) {
        Product product = inventory.getProduct();
        Warehouse warehouse = inventory.getWarehouse();

        return new InventoryResponse(
                inventory.getId(),
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getReorderLevel(),
                warehouse.getId(),
                warehouse.getName(),
                inventory.getQuantity(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt()
        );
    }

    // Create an initial inventory record
    @Transactional
    public InventoryResponse createInventory(InventoryRequest request) {

        if (request.getQuantity() == null ||
                request.getQuantity().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Quantity cannot be negative or null"
            );
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Product not found with ID: " + request.getProductId()
                ));

        Warehouse warehouse = warehouseRepository.findById(
                request.getWarehouseId()
        ).orElseThrow(() -> new IllegalArgumentException(
                "Warehouse not found with ID: " + request.getWarehouseId()
        ));

        Optional<Inventory> existingInventory =
                inventoryRepository.findByProductAndWarehouse(
                        product, warehouse
                );

        if (existingInventory.isPresent()) {
            throw new IllegalArgumentException(
                    "Inventory already exists for this product and warehouse"
            );
        }

        Inventory inventory = new Inventory(
                product,
                warehouse,
                request.getQuantity()
        );

        Inventory savedInventory = inventoryRepository.save(inventory);

        return mapToResponse(savedInventory);
    }

    // Retrieve all inventory records, including records for inactive products
    @Transactional(readOnly = true)
    public List<InventoryResponse> getAllInventory() {

        return inventoryRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Retrieve inventory by ID
    @Transactional(readOnly = true)
    public Optional<InventoryResponse> getInventoryById(Long id) {

        return inventoryRepository.findById(id)
                .map(this::mapToResponse);
    }

    // Retrieve inventory for a particular product
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByProductId(Long productId) {

        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException(
                    "Product not found with ID: " + productId
            );
        }

        return inventoryRepository.findByProductId(productId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // Retrieve inventory for a particular warehouse
    @Transactional(readOnly = true)
    public List<InventoryResponse> getInventoryByWarehouseId(Long warehouseId) {

        if (!warehouseRepository.existsById(warehouseId)) {
            throw new IllegalArgumentException(
                    "Warehouse not found with ID: " + warehouseId
            );
        }

        return inventoryRepository.findByWarehouseId(warehouseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}

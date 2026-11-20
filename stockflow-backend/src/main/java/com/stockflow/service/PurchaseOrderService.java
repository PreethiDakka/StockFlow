
package com.stockflow.service;

import com.stockflow.dto.PurchaseOrderItemRequest;
import com.stockflow.dto.PurchaseOrderItemResponse;
import com.stockflow.dto.PurchaseOrderRequest;
import com.stockflow.dto.PurchaseOrderResponse;
import com.stockflow.entity.Inventory;
import com.stockflow.entity.Product;
import com.stockflow.entity.PurchaseOrder;
import com.stockflow.entity.PurchaseOrderItem;
import com.stockflow.entity.StockMovement;
import com.stockflow.entity.Supplier;
import com.stockflow.entity.Warehouse;
import com.stockflow.repository.InventoryRepository;
import com.stockflow.repository.ProductRepository;
import com.stockflow.repository.PurchaseOrderItemRepository;
import com.stockflow.repository.PurchaseOrderRepository;
import com.stockflow.repository.StockMovementRepository;
import com.stockflow.repository.SupplierRepository;
import com.stockflow.repository.WarehouseRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            ProductRepository productRepository,
            SupplierRepository supplierRepository,
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository) {

        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.productRepository = productRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
    }

    // =========================
    // CREATE PURCHASE ORDER
    // =========================

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(
            PurchaseOrderRequest request) {

        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Purchase order request cannot be empty");
        }

        if (request.getSupplierId() == null
                || request.getSupplierId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid supplier ID is required");
        }

        if (request.getWarehouseId() == null
                || request.getWarehouseId() <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A valid warehouse ID is required");
        }

        if (request.getItems() == null
                || request.getItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A purchase order must contain at least one item");
        }

        if (request.getExpectedDeliveryDate() != null
                && request.getExpectedDeliveryDate()
                        .isBefore(LocalDate.now())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Expected delivery date cannot be in the past");
        }

        if (request.getNotes() != null
                && request.getNotes().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Notes cannot exceed 500 characters");
        }

        // Validate supplier.
        Supplier supplier = supplierRepository
                .findById(request.getSupplierId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Supplier not found"));

        if (!supplier.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot create purchase order for an inactive supplier");
        }

        // Validate warehouse.
        Warehouse warehouse = warehouseRepository
                .findById(request.getWarehouseId())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Warehouse not found"));

        if (!Boolean.TRUE.equals(warehouse.getActive())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot create purchase order for an inactive warehouse");
        }

        // Create purchase order.
        PurchaseOrder order = new PurchaseOrder();

        order.setOrderNumber(
                "PO-" + UUID.randomUUID().toString()
                        .substring(0, 8).toUpperCase());

        order.setSupplier(supplier);
        order.setWarehouse(warehouse);
        order.setStatus("DRAFT");

        order.setExpectedDeliveryDate(
                request.getExpectedDeliveryDate());

        order.setNotes(
                request.getNotes() == null
                        ? null
                        : request.getNotes().trim());

        order.setTotalAmount(BigDecimal.ZERO);

        PurchaseOrder savedOrder =
                purchaseOrderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;

        List<PurchaseOrderItem> orderItems = new ArrayList<>();

        Set<Long> productIds = new HashSet<>();

        // Validate and process each purchase order item.
        for (PurchaseOrderItemRequest itemRequest : request.getItems()) {

            if (itemRequest == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Purchase order item cannot be empty");
            }

            Long productId = itemRequest.getProductId();

            if (productId == null || productId <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "A valid product ID is required");
            }

            if (!productIds.add(productId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Duplicate products are not allowed in one order");
            }

            BigDecimal requestedQuantity =
                    itemRequest.getQuantity();

            if (requestedQuantity == null
                    || requestedQuantity.compareTo(
                            BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Quantity must be greater than zero");
            }

            BigDecimal requestedUnitPrice =
                    itemRequest.getUnitPrice();

            if (requestedUnitPrice == null
                    || requestedUnitPrice.compareTo(
                            BigDecimal.ZERO) <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unit price must be greater than zero");
            }

            // Validate quantity precision: maximum 3 decimal places.
            BigDecimal quantity;

            try {
                quantity = requestedQuantity.setScale(
                        3, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Quantity cannot have more than 3 decimal places");
            }

            // Validate unit price precision: maximum 2 decimal places.
            BigDecimal unitPrice;

            try {
                unitPrice = requestedUnitPrice.setScale(
                        2, RoundingMode.UNNECESSARY);
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unit price cannot have more than 2 decimal places");
            }

            // Check whether product exists and is active.
            Product product = productRepository
                    .findByIdAndActiveTrue(productId)
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.NOT_FOUND,
                                    "Product not found or inactive: "
                                            + productId));

            // Calculate line total.
            BigDecimal lineTotal = quantity
                    .multiply(unitPrice)
                    .setScale(2, RoundingMode.HALF_UP);

            PurchaseOrderItem orderItem =
                    new PurchaseOrderItem();

            orderItem.setPurchaseOrder(savedOrder);
            orderItem.setProduct(product);
            orderItem.setQuantity(quantity);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setLineTotal(lineTotal);

            orderItems.add(orderItem);

            totalAmount = totalAmount.add(lineTotal);
        }

        // Update total amount.
        savedOrder.setTotalAmount(
                totalAmount.setScale(2, RoundingMode.HALF_UP));

        purchaseOrderRepository.save(savedOrder);

        purchaseOrderItemRepository.saveAll(orderItems);

        return mapToResponse(savedOrder, orderItems);
    }

    // =========================
    // GET ALL PURCHASE ORDERS
    // =========================

    @Transactional(readOnly = true)
    public Page<PurchaseOrderResponse> getAllPurchaseOrders(
            Pageable pageable) {

        return purchaseOrderRepository.findAll(pageable)
                .map(order -> mapToResponse(
                        order,
                        purchaseOrderItemRepository
                                .findByPurchaseOrderId(order.getId())));
    }

    // =========================
    // GET PURCHASE ORDER BY ID
    // =========================

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {

        PurchaseOrder order = getOrder(id);

        List<PurchaseOrderItem> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrderId(id);

        return mapToResponse(order, items);
    }

    // =========================
    // MARK ORDER AS ORDERED
    // =========================

    @Transactional
    public PurchaseOrderResponse markAsOrdered(Long id) {

        PurchaseOrder order = getOrder(id);

        if (!"DRAFT".equals(order.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only draft purchase orders can be marked as ordered");
        }

        order.setStatus("ORDERED");

        PurchaseOrder savedOrder =
                purchaseOrderRepository.save(order);

        return mapToResponse(
                savedOrder,
                purchaseOrderItemRepository.findByPurchaseOrderId(id));
    }

    // =========================
    // CANCEL PURCHASE ORDER
    // =========================

    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long id) {

        PurchaseOrder order = getOrder(id);

        if (!"DRAFT".equals(order.getStatus())
                && !"ORDERED".equals(order.getStatus())) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This purchase order cannot be cancelled");
        }

        order.setStatus("CANCELLED");

        PurchaseOrder savedOrder =
                purchaseOrderRepository.save(order);

        return mapToResponse(
                savedOrder,
                purchaseOrderItemRepository.findByPurchaseOrderId(id));
    }

    // =========================
    // RECEIVE PURCHASE ORDER
    // =========================

    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long id) {

        // Lock purchase order row to prevent concurrent receiving.
        PurchaseOrder order = purchaseOrderRepository
                .findByIdForUpdate(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Purchase order not found"));

        if (!"ORDERED".equals(order.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only ordered purchase orders can be received");
        }

        Warehouse warehouse = order.getWarehouse();

        List<PurchaseOrderItem> items =
                purchaseOrderItemRepository
                        .findByPurchaseOrderId(id);

        if (items.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Purchase order has no items");
        }

        // Lock inventory rows in ascending product ID order.
        List<PurchaseOrderItem> sortedItems =
                new ArrayList<>(items);

        sortedItems.sort(
                (a, b) -> a.getProduct().getId()
                        .compareTo(b.getProduct().getId()));

        for (PurchaseOrderItem item : sortedItems) {

            Product product = item.getProduct();

            Inventory inventory =
                    inventoryRepository
                            .findByProductAndWarehouseForUpdate(
                                    product.getId(),
                                    warehouse.getId())
                            .orElseGet(() ->
                                    new Inventory(
                                            product,
                                            warehouse,
                                            BigDecimal.ZERO));

            inventory.setQuantity(
                    inventory.getQuantity()
                            .add(item.getQuantity()));

            inventoryRepository.save(inventory);

            StockMovement movement = new StockMovement();

            movement.setProduct(product);
            movement.setWarehouse(warehouse);
            movement.setMovementType("STOCK_IN");
            movement.setQuantity(item.getQuantity());
            movement.setReference(order.getOrderNumber());

            movement.setRemarks(
                    "Purchase order received: "
                            + order.getOrderNumber());

            stockMovementRepository.save(movement);
        }

        order.setStatus("RECEIVED");
        order.setReceivedAt(LocalDateTime.now());

        PurchaseOrder savedOrder =
                purchaseOrderRepository.save(order);

        return mapToResponse(savedOrder, items);
    }

    // =========================
    // HELPER: GET ORDER
    // =========================

    private PurchaseOrder getOrder(Long id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid purchase order ID");
        }

        return purchaseOrderRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Purchase order not found"));
    }

    // =========================
    // MAP RESPONSE
    // =========================

    private PurchaseOrderResponse mapToResponse(
            PurchaseOrder order,
            List<PurchaseOrderItem> items) {

        PurchaseOrderResponse response =
                new PurchaseOrderResponse();

        response.setId(order.getId());
        response.setOrderNumber(order.getOrderNumber());

        response.setSupplierId(order.getSupplier().getId());
        response.setSupplierName(order.getSupplier().getName());

        response.setWarehouseId(order.getWarehouse().getId());
        response.setWarehouseName(order.getWarehouse().getName());

        response.setStatus(order.getStatus());
        response.setOrderDate(order.getOrderDate());

        response.setExpectedDeliveryDate(
                order.getExpectedDeliveryDate());

        response.setReceivedAt(order.getReceivedAt());
        response.setTotalAmount(order.getTotalAmount());
        response.setNotes(order.getNotes());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        List<PurchaseOrderItemResponse> itemResponses =
                new ArrayList<>();

        for (PurchaseOrderItem item : items) {

            PurchaseOrderItemResponse itemResponse =
                    new PurchaseOrderItemResponse();

            itemResponse.setId(item.getId());
            itemResponse.setProductId(item.getProduct().getId());
            itemResponse.setProductName(item.getProduct().getName());
            itemResponse.setProductSku(item.getProduct().getSku());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setUnitPrice(item.getUnitPrice());
            itemResponse.setLineTotal(item.getLineTotal());

            itemResponses.add(itemResponse);
        }

        response.setItems(itemResponses);

        return response;
    }
}

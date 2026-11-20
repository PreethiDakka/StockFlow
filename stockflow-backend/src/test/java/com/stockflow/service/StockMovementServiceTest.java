
package com.stockflow.service;

import com.stockflow.dto.*;
import com.stockflow.entity.*;
import com.stockflow.repository.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    private Product product;
    private Warehouse warehouse1;
    private Warehouse warehouse2;

    @BeforeEach
void setUp() {
    product = mock(Product.class);
    lenient().when(product.getId()).thenReturn(1L);
    lenient().when(product.getName()).thenReturn("Wireless Keyboard");
    lenient().when(product.getSku()).thenReturn("WK-001");

    warehouse1 = mock(Warehouse.class);
    lenient().when(warehouse1.getId()).thenReturn(1L);
    lenient().when(warehouse1.getName()).thenReturn("Hyderabad Warehouse");
        lenient().when(warehouse1.getActive()).thenReturn(true);
    warehouse2 = mock(Warehouse.class);
    lenient().when(warehouse2.getId()).thenReturn(2L);
    lenient().when(warehouse2.getName()).thenReturn("Vizag Warehouse");

lenient().when(warehouse2.getActive()).thenReturn(true);

    lenient().when(productRepository.findByIdAndActiveTrue(1L))
            .thenReturn(Optional.of(product));

    lenient().when(warehouseRepository.findById(1L))
            .thenReturn(Optional.of(warehouse1));

    lenient().when(warehouseRepository.findById(2L))
            .thenReturn(Optional.of(warehouse2));

    lenient().when(inventoryRepository.save(any(Inventory.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    lenient().when(stockMovementRepository.save(any(StockMovement.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
}

    private StockInRequest stockInRequest(String quantity) {
        StockInRequest request = new StockInRequest();
        request.setProductId(1L);
        request.setWarehouseId(1L);
        request.setQuantity(new BigDecimal(quantity));
        request.setReference("PO-1001");
        request.setRemarks("Test stock receipt");
        return request;
    }

    private StockOutRequest stockOutRequest(String quantity) {
        StockOutRequest request = new StockOutRequest();
        request.setProductId(1L);
        request.setWarehouseId(1L);
        request.setQuantity(new BigDecimal(quantity));
        request.setReference("SO-1001");
        request.setRemarks("Test stock dispatch");
        return request;
    }

    private StockTransferRequest transferRequest(
            Long sourceId, Long destinationId, String quantity) {

        StockTransferRequest request = new StockTransferRequest();
        request.setProductId(1L);
        request.setSourceWarehouseId(sourceId);
        request.setDestinationWarehouseId(destinationId);
        request.setQuantity(new BigDecimal(quantity));
        request.setReference("TR-1001");
        request.setRemarks("Test warehouse transfer");
        return request;
    }

    private StockMovement createMovement(String type, String quantity) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setWarehouse(warehouse1);
        movement.setMovementType(type);
        movement.setQuantity(new BigDecimal(quantity));
        movement.setReference("REF-1001");
        movement.setRemarks("Test movement");
        return movement;
    }

    // =========================
    // STOCK IN TESTS
    // =========================

    @Test
    void shouldCreateStockInSuccessfully() {
        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        StockInResponse response =
                stockMovementService.stockIn(stockInRequest("50"));

        assertEquals(1L, response.getProductId());
        assertEquals("Wireless Keyboard", response.getProductName());
        assertEquals("STOCK_IN", response.getMovementType());
        assertEquals(new BigDecimal("50"), response.getQuantityAdded());
        assertEquals(
                new BigDecimal("50"),
                response.getUpdatedInventoryQuantity());

        verify(inventoryRepository).save(any(Inventory.class));
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void shouldAddStockToExistingInventory() {
        Inventory inventory = new Inventory(
                product, warehouse1, new BigDecimal("100"));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));

        StockInResponse response =
                stockMovementService.stockIn(stockInRequest("25"));

        assertEquals(
                new BigDecimal("125"),
                response.getUpdatedInventoryQuantity());

        verify(inventoryRepository).save(inventory);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void shouldRejectStockInWhenProductDoesNotExist() {
        when(productRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        StockInRequest request = stockInRequest("50");
        request.setProductId(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.stockIn(request));

        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    @Test
    void shouldRejectStockInWhenProductIsInactive() {
        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.stockIn(stockInRequest("10")));

        verify(inventoryRepository, never())
                .save(any(Inventory.class));
        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    @Test
    void shouldRejectStockInWhenWarehouseDoesNotExist() {
        when(warehouseRepository.findById(99L))
                .thenReturn(Optional.empty());

        StockInRequest request = stockInRequest("50");
        request.setWarehouseId(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.stockIn(request));

        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    // =========================
    // STOCK OUT TESTS
    // =========================

    @Test
    void shouldCreateStockOutSuccessfully() {
        Inventory inventory = new Inventory(
                product, warehouse1, new BigDecimal("100"));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));

        StockOutResponse response =
                stockMovementService.stockOut(stockOutRequest("30"));

        assertEquals("STOCK_OUT", response.getMovementType());
        assertEquals(new BigDecimal("30"), response.getQuantityRemoved());
        assertEquals(new BigDecimal("70"), response.getRemainingQuantity());

        verify(inventoryRepository).save(inventory);
        verify(stockMovementRepository).save(any(StockMovement.class));
    }

    @Test
    void shouldRejectStockOutWhenQuantityIsInsufficient() {
        Inventory inventory = new Inventory(
                product, warehouse1, new BigDecimal("20"));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.of(inventory));

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.stockOut(stockOutRequest("50")));

        assertEquals(new BigDecimal("20"), inventory.getQuantity());

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    @Test
    void shouldRejectStockOutWhenInventoryDoesNotExist() {
        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.stockOut(stockOutRequest("10")));

        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    // =========================
    // STOCK TRANSFER TESTS
    // =========================

    @Test
    void shouldTransferStockSuccessfully() {
        Inventory sourceInventory = new Inventory(
                product, warehouse1, new BigDecimal("100"));

        Inventory destinationInventory = new Inventory(
                product, warehouse2, new BigDecimal("40"));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.of(sourceInventory));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 2L))
                .thenReturn(Optional.of(destinationInventory));

        StockTransferResponse response =
                stockMovementService.transferStock(
                        transferRequest(1L, 2L, "25"));

        assertEquals(1L, response.getProductId());
        assertEquals("Wireless Keyboard", response.getProductName());

        assertEquals(new BigDecimal("25"), response.getQuantityTransferred());
        assertEquals(
                new BigDecimal("75"),
                response.getSourceRemainingQuantity());
        assertEquals(
                new BigDecimal("65"),
                response.getDestinationUpdatedQuantity());

        assertEquals(new BigDecimal("75"), sourceInventory.getQuantity());
        assertEquals(new BigDecimal("65"), destinationInventory.getQuantity());

        verify(inventoryRepository, times(2))
                .save(any(Inventory.class));

        verify(stockMovementRepository, times(2))
                .save(any(StockMovement.class));
    }

    @Test
    void shouldRejectTransferToSameWarehouse() {
        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.transferStock(
                        transferRequest(1L, 1L, "10")));

        verify(inventoryRepository, never())
                .save(any(Inventory.class));

        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    @Test
    void shouldRejectTransferWhenSourceStockIsInsufficient() {
        Inventory sourceInventory = new Inventory(
                product, warehouse1, new BigDecimal("10"));

        Inventory destinationInventory = new Inventory(
                product, warehouse2, new BigDecimal("20"));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.of(sourceInventory));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 2L))
                .thenReturn(Optional.of(destinationInventory));

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.transferStock(
                        transferRequest(1L, 2L, "50")));

        assertEquals(new BigDecimal("10"), sourceInventory.getQuantity());
        assertEquals(new BigDecimal("20"), destinationInventory.getQuantity());

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    @Test
    void shouldRejectTransferWhenSourceInventoryDoesNotExist() {
        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.transferStock(
                        transferRequest(1L, 2L, "10")));

        verify(stockMovementRepository, never())
                .save(any(StockMovement.class));
    }

    @Test
    void shouldCreateDestinationInventoryWhenItDoesNotExist() {
        Inventory sourceInventory = new Inventory(
                product, warehouse1, new BigDecimal("100"));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 1L))
                .thenReturn(Optional.of(sourceInventory));

        when(inventoryRepository.findByProductAndWarehouseForUpdate(1L, 2L))
                .thenReturn(Optional.empty());

        StockTransferResponse response =
                stockMovementService.transferStock(
                        transferRequest(1L, 2L, "10"));

        assertEquals(new BigDecimal("90"), sourceInventory.getQuantity());
        assertEquals(new BigDecimal("10"), response.getDestinationUpdatedQuantity());
        assertEquals(new BigDecimal("90"), response.getSourceRemainingQuantity());

        org.mockito.ArgumentCaptor<Inventory> inventoryCaptor =
                org.mockito.ArgumentCaptor.forClass(Inventory.class);

        verify(inventoryRepository, times(2)).save(inventoryCaptor.capture());

        Inventory savedDestination = inventoryCaptor.getAllValues().get(1);
        assertEquals(2L, savedDestination.getWarehouse().getId());
        assertEquals(new BigDecimal("10"), savedDestination.getQuantity());

        verify(stockMovementRepository, times(2))
                .save(any(StockMovement.class));
    }

    // =========================
    // STOCK MOVEMENT HISTORY TESTS
    // =========================

    @Test
    void shouldReturnAllMovements() {
        StockMovement movement = createMovement("STOCK_IN", "50");

        Page<StockMovement> movementPage =
                new PageImpl<>(List.of(movement));

        when(stockMovementRepository.findAll(any(PageRequest.class)))
                .thenReturn(movementPage);

        Page<StockMovementResponse> response =
                stockMovementService.getAllMovements(
                        PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());
        assertEquals("STOCK_IN", response.getContent().get(0).getMovementType());
    }

    @Test
    void shouldReturnMovementById() {
        StockMovement movement = createMovement("STOCK_IN", "50");

        when(stockMovementRepository.findById(1L))
                .thenReturn(Optional.of(movement));

        StockMovementResponse response =
                stockMovementService.getMovementById(1L);

        assertEquals("STOCK_IN", response.getMovementType());
        assertEquals(1L, response.getProductId());
        assertEquals("Wireless Keyboard", response.getProductName());
    }

    @Test
    void shouldRejectWhenMovementIdDoesNotExist() {
        when(stockMovementRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.getMovementById(99L));
    }

    @Test
    void shouldReturnMovementsByProduct() {
        StockMovement movement = createMovement("STOCK_IN", "50");

        when(productRepository.existsById(1L)).thenReturn(true);

        when(stockMovementRepository.findByProductId(
                eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));

        Page<StockMovementResponse> response =
                stockMovementService.getMovementsByProduct(
                        1L, PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());

        verify(stockMovementRepository)
                .findByProductId(eq(1L), any(PageRequest.class));
    }

    @Test
    void shouldRejectMovementsByNonexistentProduct() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.getMovementsByProduct(
                        99L, PageRequest.of(0, 10)));

        verify(stockMovementRepository, never())
                .findByProductId(anyLong(), any(PageRequest.class));
    }

    @Test
    void shouldReturnMovementsByWarehouse() {
        StockMovement movement = createMovement("STOCK_OUT", "20");

        when(warehouseRepository.existsById(1L)).thenReturn(true);

        when(stockMovementRepository.findByWarehouseId(
                eq(1L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));

        Page<StockMovementResponse> response =
                stockMovementService.getMovementsByWarehouse(
                        1L, PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());

        verify(stockMovementRepository)
                .findByWarehouseId(eq(1L), any(PageRequest.class));
    }

    @Test
    void shouldRejectMovementsByNonexistentWarehouse() {
        when(warehouseRepository.existsById(99L)).thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.getMovementsByWarehouse(
                        99L, PageRequest.of(0, 10)));

        verify(stockMovementRepository, never())
                .findByWarehouseId(anyLong(), any(PageRequest.class));
    }

    @Test
    void shouldReturnMovementsByTypeIgnoringCase() {
        StockMovement movement = createMovement("STOCK_IN", "50");

        when(stockMovementRepository.findByMovementType(
                eq("STOCK_IN"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(movement)));

        Page<StockMovementResponse> response =
                stockMovementService.getMovementsByType(
                        "stock_in", PageRequest.of(0, 10));

        assertEquals(1, response.getTotalElements());

        verify(stockMovementRepository)
                .findByMovementType(eq("STOCK_IN"), any(PageRequest.class));
    }

    @Test
    void shouldRejectInvalidMovementType() {
        assertThrows(
                IllegalArgumentException.class,
                () -> stockMovementService.getMovementsByType(
                        "INVALID", PageRequest.of(0, 10)));

        verify(stockMovementRepository, never())
                .findByMovementType(anyString(), any(PageRequest.class));
    }
}

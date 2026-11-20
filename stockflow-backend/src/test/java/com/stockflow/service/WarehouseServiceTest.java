package com.stockflow.service;

import com.stockflow.dto.WarehouseRequest;
import com.stockflow.dto.WarehouseResponse;
import com.stockflow.entity.Warehouse;
import com.stockflow.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private WarehouseRequest request;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        request = new WarehouseRequest();
        request.setName("Hyderabad Central Warehouse");
        request.setCode("HYD-001");
        request.setAddress("Gachibowli");
        request.setCity("Hyderabad");
        request.setState("Telangana");
        request.setCountry("India");

        warehouse = new Warehouse(
                "Hyderabad Central Warehouse", "HYD-001", "Gachibowli",
                "Hyderabad", "Telangana", "India"
        );
        //warehouse.setId(1L);
        ReflectionTestUtils.setField(warehouse, "id", 1L);
        warehouse.setActive(true);
    }

    @Test
    void createWarehouse_shouldSaveAndReturnWarehouse() {
        when(warehouseRepository.existsByCode("HYD-001")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WarehouseResponse response = warehouseService.createWarehouse(request);

        assertNotNull(response);
        assertEquals("Hyderabad Central Warehouse", response.name());
        assertEquals("HYD-001", response.code());
        assertEquals("Hyderabad", response.city());
        assertEquals("India", response.country());
        verify(warehouseRepository).existsByCode("HYD-001");
        verify(warehouseRepository).save(any(Warehouse.class));
    }

    @Test
    void createWarehouse_shouldThrowWhenCodeAlreadyExists() {
        when(warehouseRepository.existsByCode("HYD-001")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> warehouseService.createWarehouse(request));

        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void getAllWarehouses_shouldReturnAllWarehouses() {
        when(warehouseRepository.findAll()).thenReturn(List.of(warehouse));

        List<WarehouseResponse> response = warehouseService.getAllWarehouses();

        assertEquals(1, response.size());
        assertEquals("HYD-001", response.get(0).code());
        verify(warehouseRepository).findAll();
    }

    @Test
    void getWarehouseById_shouldReturnWarehouseWhenFound() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));

        Optional<WarehouseResponse> response = warehouseService.getWarehouseById(1L);

        assertTrue(response.isPresent());
        assertEquals(1L, response.get().id());
        assertEquals("HYD-001", response.get().code());
        verify(warehouseRepository).findById(1L);
    }

    @Test
    void getWarehouseById_shouldReturnEmptyWhenNotFound() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<WarehouseResponse> response = warehouseService.getWarehouseById(99L);

        assertTrue(response.isEmpty());
        verify(warehouseRepository).findById(99L);
    }

    @Test
    void updateWarehouse_shouldUpdateAndReturnWarehouse() {
        request.setAddress("Madhapur");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByCodeAndIdNot("HYD-001", 1L)).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<WarehouseResponse> response = warehouseService.updateWarehouse(1L, request);

        assertTrue(response.isPresent());
        assertEquals("Madhapur", response.get().address());
        assertEquals("HYD-001", response.get().code());
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void updateWarehouse_shouldThrowWhenCodeBelongsToAnotherWarehouse() {
        request.setCode("BLR-001");
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.existsByCodeAndIdNot("BLR-001", 1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> warehouseService.updateWarehouse(1L, request));

        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void updateWarehouse_shouldReturnEmptyWhenWarehouseDoesNotExist() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<WarehouseResponse> response = warehouseService.updateWarehouse(99L, request);

        assertTrue(response.isEmpty());
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }

    @Test
    void deactivateWarehouse_shouldSetActiveToFalse() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.save(any(Warehouse.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        boolean result = warehouseService.deactivateWarehouse(1L);

        assertTrue(result);
        assertFalse(warehouse.getActive());
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void deactivateWarehouse_shouldReturnFalseWhenWarehouseDoesNotExist() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        boolean result = warehouseService.deactivateWarehouse(99L);

        assertFalse(result);
        verify(warehouseRepository, never()).save(any(Warehouse.class));
    }
}

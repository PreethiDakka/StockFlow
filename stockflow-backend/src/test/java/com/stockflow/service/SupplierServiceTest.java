
package com.stockflow.service;

import com.stockflow.dto.SupplierRequest;
import com.stockflow.dto.SupplierResponse;
import com.stockflow.entity.Supplier;
import com.stockflow.repository.SupplierRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier supplier;
    private SupplierRequest request;

    @BeforeEach
    void setUp() {
        request = new SupplierRequest(
                "ABC Electronics",
                "SUP-001",
                "John",
                "john@example.com",
                "9876543210",
                "Main Road",
                "Hyderabad",
                "Telangana",
                "India"
        );

        supplier = new Supplier();
        supplier.setName("ABC Electronics");
        supplier.setCode("SUP-001");
        supplier.setContactPerson("John");
        supplier.setEmail("john@example.com");
        supplier.setPhone("9876543210");
        supplier.setAddress("Main Road");
        supplier.setCity("Hyderabad");
        supplier.setState("Telangana");
        supplier.setCountry("India");
        supplier.setActive(true);
    }

    @Test
    void createSupplierSuccessfully() {
        when(supplierRepository.existsByCode("SUP-001"))
                .thenReturn(false);

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse response = supplierService.create(request);

        assertNotNull(response);
        assertEquals("ABC Electronics", response.name());
        assertEquals("SUP-001", response.code());
        assertTrue(response.active());

        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createSupplierWithDuplicateCodeThrowsException() {
        when(supplierRepository.existsByCode("SUP-001"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.create(request)
        );

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void getAllSuppliersSuccessfully() {
        when(supplierRepository.findAll())
                .thenReturn(List.of(supplier));

        List<SupplierResponse> responses = supplierService.getAll();

        assertEquals(1, responses.size());
        assertEquals("ABC Electronics", responses.get(0).name());
        assertEquals("SUP-001", responses.get(0).code());

        verify(supplierRepository).findAll();
    }

    @Test
    void getSupplierByIdSuccessfully() {
        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        SupplierResponse response = supplierService.getById(1L);

        assertNotNull(response);
        assertEquals("ABC Electronics", response.name());
        assertEquals("SUP-001", response.code());
    }

    @Test
    void getSupplierByIdWhenNotFoundThrowsException() {
        when(supplierRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> supplierService.getById(99L)
        );
    }

    @Test
    void updateSupplierSuccessfully() {
        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.existsByCodeAndIdNot("SUP-001", 1L))
                .thenReturn(false);

        when(supplierRepository.saveAndFlush(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SupplierResponse response = supplierService.update(1L, request);

        assertNotNull(response);
        assertEquals("ABC Electronics", response.name());
        assertEquals("SUP-001", response.code());

        verify(supplierRepository).saveAndFlush(supplier);
    }

    @Test
    void updateSupplierWithDuplicateCodeThrowsException() {
        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.existsByCodeAndIdNot("SUP-001", 1L))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> supplierService.update(1L, request)
        );

        verify(supplierRepository, never())
                .saveAndFlush(any(Supplier.class));
    }

    @Test
    void deactivateSupplierSuccessfully() {
        when(supplierRepository.findById(1L))
                .thenReturn(Optional.of(supplier));

        when(supplierRepository.save(any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.deactivate(1L);

        assertFalse(supplier.isActive());

        verify(supplierRepository).save(supplier);
    }
}

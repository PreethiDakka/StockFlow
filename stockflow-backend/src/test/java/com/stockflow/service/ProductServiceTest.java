
package com.stockflow.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.test.util.ReflectionTestUtils;
import static org.mockito.ArgumentMatchers.anyLong;


import com.stockflow.dto.ProductRequest;
import com.stockflow.dto.ProductResponse;
import com.stockflow.entity.Product;
import com.stockflow.repository.ProductRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

//import java.math.BigDecimal;
import java.util.List;
//import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        productRequest = new ProductRequest();

        productRequest.setName("Wireless Mouse");
        productRequest.setSku("WM-001");
        productRequest.setDescription("Wireless optical mouse");
        productRequest.setPrice(new BigDecimal("599.00"));
        productRequest.setReorderLevel(10);
    }

    // Test 1: Create product successfully
    @Test
    void shouldCreateProductSuccessfully() {

        when(productRepository.existsBySku("WM-001"))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response =
                productService.createProduct(productRequest);

        assertEquals("Wireless Mouse", response.name());
        assertEquals("WM-001", response.sku());
        assertEquals(new BigDecimal("599.00"), response.price());

        verify(productRepository).save(any(Product.class));
    }

    // Test 2: Reject duplicate SKU
    @Test
    void shouldRejectDuplicateSku() {

        when(productRepository.existsBySku("WM-001"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(productRequest)
        );

        verify(productRepository, never()).save(any(Product.class));
    }

    // Test 3: Get all products
    @Test
    void shouldReturnAllProducts() {

        List<Product> products = List.of(
                new Product(
                        "Wireless Mouse",
                        "WM-001",
                        "Wireless optical mouse",
                        new BigDecimal("599.00"),
                        10
                ),
                new Product(
                        "USB Hub",
                        "USB-001",
                        "Four port USB hub",
                        new BigDecimal("499.00"),
                        5
                )
        );

        when(productRepository.findAllByActiveTrue()).thenReturn(products);

        List<ProductResponse> response =
                productService.getAllProducts();

        assertEquals(2, response.size());
        assertEquals("Wireless Mouse", response.get(0).name());
        assertEquals("USB Hub", response.get(1).name());
    }

    // Test 4: Get product by ID
    @Test
    void shouldReturnProductById() {

        Product product = new Product(
                "Wireless Mouse",
                "WM-001",
                "Wireless optical mouse",
                new BigDecimal("599.00"),
                10
        );

        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(product));

        Optional<ProductResponse> response =
                productService.getProductById(1L);

        assertTrue(response.isPresent());
        assertEquals("WM-001", response.get().sku());
    }

    // Test 5: Return empty for nonexistent product
    @Test
    void shouldReturnEmptyWhenProductDoesNotExist() {

        when(productRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        Optional<ProductResponse> response =
                productService.getProductById(99L);

        assertTrue(response.isEmpty());
    }

    // Test 6: Update product successfully
    @Test
    void shouldUpdateProductSuccessfully() {

        Product existingProduct = new Product(
                "Wireless Mouse",
                "WM-001",
                "Old description",
                new BigDecimal("599.00"),
                10
        );

        productRequest.setDescription("Updated description");
        productRequest.setPrice(new BigDecimal("699.00"));

        when(productRepository.findByIdAndActiveTrue(1L))
                .thenReturn(Optional.of(existingProduct));

        when(productRepository.existsBySkuAndIdNot("WM-001", 1L))
                .thenReturn(false);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<ProductResponse> response =
                productService.updateProduct(1L, productRequest);

        assertTrue(response.isPresent());
        assertEquals("Updated description", response.get().description());
        assertEquals(new BigDecimal("699.00"), response.get().price());
    }

    // Test 7: Return empty when updating nonexistent product
    @Test
    void shouldReturnEmptyWhenUpdatingNonexistentProduct() {

        when(productRepository.findByIdAndActiveTrue(99L))
                .thenReturn(Optional.empty());

        Optional<ProductResponse> response =
                productService.updateProduct(99L, productRequest);

        assertTrue(response.isEmpty());

        verify(productRepository, never()).save(any(Product.class));
    }

    // Test 8: Deactivate product successfully
@Test
void shouldDeactivateProductSuccessfully() {

    Product product = new Product(
            "Test Product",
            "TEST-001",
            "Test Description",
            new BigDecimal("100.00"),
            10
    );

    ReflectionTestUtils.setField(product, "id", 1L);

    when(productRepository.findByIdAndActiveTrue(1L))
            .thenReturn(Optional.of(product));

    when(productRepository.save(any(Product.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

    boolean result = productService.deactivateProduct(1L);

    assertTrue(result);
    assertFalse(product.isActive());

    verify(productRepository).save(product);
    verify(productRepository, never()).deleteById(anyLong());
}

// Test 9: Handle deactivation of nonexistent product
@Test
void shouldNotDeactivateNonexistentProduct() {

    when(productRepository.findByIdAndActiveTrue(99L))
            .thenReturn(Optional.empty());

    boolean result = productService.deactivateProduct(99L);

    assertFalse(result);

    verify(productRepository, never()).save(any(Product.class));
    verify(productRepository, never()).deleteById(anyLong());
}
}

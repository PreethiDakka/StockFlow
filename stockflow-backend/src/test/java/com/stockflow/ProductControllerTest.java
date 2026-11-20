
package com.stockflow;

//import org.springframework.security.test.context.support.WithMockUser;

import com.stockflow.dto.ProductRequest;
import com.stockflow.repository.ProductRepository;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
//@WithMockUser(username = "stockflowuser")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    // Clean database before every test
    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    // Helper 1: Create a ProductRequest
    private ProductRequest createRequest(
            String name,
            String sku,
            String description,
            String price,
            Integer reorderLevel) {

        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setSku(sku);
        request.setDescription(description);
        request.setPrice(new java.math.BigDecimal(price));
        request.setReorderLevel(reorderLevel);

        return request;
    }

    // Helper 2: Convert Java object into JSON
    private String toJson(ProductRequest request) throws Exception {
        return objectMapper.writeValueAsString(request);
    }

    // Helper 3: Create product and return generated ID
    private Long createProduct(ProductRequest request) throws Exception {

        String response = mockMvc.perform(
                post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
        )
        .andExpect(status().isCreated())
        .andReturn()
        .getResponse()
        .getContentAsString();

        return objectMapper.readTree(response)
                .get("id")
                .asLong();
    }

    // 1. Create product
    @Test
    void shouldCreateProductSuccessfully() throws Exception {

        ProductRequest request = createRequest(
                "Wireless Mouse",
                "WM-101",
                "Wireless mouse with USB receiver",
                "599.00",
                10
        );

        mockMvc.perform(
                post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Wireless Mouse"))
        .andExpect(jsonPath("$.sku").value("WM-101"));
    }

    // 2. Reject invalid product
    @Test
    void shouldReturnBadRequestForInvalidProduct() throws Exception {

        ProductRequest request = createRequest(
                "",
                "INVALID-101",
                "Invalid product",
                "-100",
                10
        );

        mockMvc.perform(
                post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
        )
        .andExpect(status().isBadRequest());
    }

    // 3. Reject duplicate SKU
    @Test
    void shouldReturnConflictForDuplicateSku() throws Exception {

        ProductRequest firstProduct = createRequest(
                "Wireless Mouse",
                "WM-201",
                "First product",
                "599.00",
                10
        );

        createProduct(firstProduct);

        ProductRequest secondProduct = createRequest(
                "Gaming Mouse",
                "WM-201",
                "Second product",
                "999.00",
                5
        );

        mockMvc.perform(
                post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(secondProduct))
        )
        .andExpect(status().isConflict());
    }

    // 4. Get all products
    @Test
    void shouldReturnAllProducts() throws Exception {

        ProductRequest request = createRequest(
                "USB Hub",
                "USB-301",
                "4-port USB hub",
                "499.00",
                5
        );

        createProduct(request);

        mockMvc.perform(get("/api/v1/products"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("USB Hub"));
    }

    // 5. Get product by ID
    @Test
    void shouldReturnProductById() throws Exception {

        ProductRequest request = createRequest(
                "Laptop Stand",
                "LS-401",
                "Aluminium laptop stand",
                "799.00",
                8
        );

        Long productId = createProduct(request);

        mockMvc.perform(get("/api/v1/products/" + productId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Laptop Stand"));
    }

    // 6. Return 404 for nonexistent product
    @Test
    void shouldReturnNotFoundForNonexistentProduct() throws Exception {

        mockMvc.perform(get("/api/v1/products/99999"))
        .andExpect(status().isNotFound());
    }

    // 7. Update product
    @Test
    void shouldUpdateProductSuccessfully() throws Exception {

        ProductRequest originalRequest = createRequest(
                "Wireless Mouse",
                "WM-501",
                "Basic wireless mouse",
                "599.00",
                10
        );

        Long productId = createProduct(originalRequest);

        ProductRequest updatedRequest = createRequest(
                "Gaming Mouse",
                "GM-501",
                "RGB gaming mouse",
                "1299.00",
                5
        );

        mockMvc.perform(
                put("/api/v1/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updatedRequest))
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Gaming Mouse"))
        .andExpect(jsonPath("$.price").value(1299.00));
    }

    // 8. Deactivate product (soft delete)
    @Test
    void shouldDeactivateProductSuccessfully() throws Exception {

        ProductRequest request = createRequest(
                "USB Keyboard",
                "KB-601",
                "Standard USB keyboard",
                "799.00",
                5
        );

        Long productId = createProduct(request);

        mockMvc.perform(patch("/api/v1/products/" + productId + "/deactivate"))
        .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + productId))
        .andExpect(status().isNotFound());
    }

    // 9. Update nonexistent product
    @Test
    void shouldReturnNotFoundWhenUpdatingNonexistentProduct()
            throws Exception {

        ProductRequest request = createRequest(
                "New Mouse",
                "NM-701",
                "Wireless mouse",
                "599.00",
                5
        );

        mockMvc.perform(
                put("/api/v1/products/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request))
        )
        .andExpect(status().isNotFound());
    }

    // 10. Deactivate nonexistent product
    @Test
    void shouldReturnNotFoundWhenDeactivatingNonexistentProduct()
            throws Exception {

        mockMvc.perform(patch("/api/v1/products/99999/deactivate"))
        .andExpect(status().isNotFound());
    }
}

package com.stockflow.controller;
import com.stockflow.entity.Supplier;
import com.stockflow.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
//@WithMockUser(username = "stockflowuser")
class SupplierControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SupplierRepository supplierRepository;

    @BeforeEach
    void setUp() {
        supplierRepository.deleteAll();
    }

    @Test
    void createSupplier_shouldReturnCreated() throws Exception {
        String request = """
                {
                    "name": "ABC Electronics",
                    "code": "SUP-001",
                    "contactPerson": "Rahul Kumar",
                    "email": "rahul@abcelectronics.com",
                    "phone": "9876543210",
                    "address": "Madhapur",
                    "city": "Hyderabad",
                    "state": "Telangana",
                    "country": "India"
                }
                """;

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("ABC Electronics"))
                .andExpect(jsonPath("$.code").value("SUP-001"))
                .andExpect(jsonPath("$.city").value("Hyderabad"))
                .andExpect(jsonPath("$.country").value("India"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getAllSuppliers_shouldReturnSupplierList() throws Exception {
        supplierRepository.save(createSupplier("ABC Electronics", "SUP-001"));
        supplierRepository.save(createSupplier("XYZ Components", "SUP-002"));

        mockMvc.perform(get("/api/v1/suppliers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getSupplierById_shouldReturnSupplier() throws Exception {
        Supplier supplier = supplierRepository.save(
                createSupplier("ABC Electronics", "SUP-001")
        );

        mockMvc.perform(get("/api/v1/suppliers/{id}", supplier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(supplier.getId()))
                .andExpect(jsonPath("$.name").value("ABC Electronics"))
                .andExpect(jsonPath("$.code").value("SUP-001"));
    }

    @Test
    void getSupplierById_whenNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/suppliers/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSupplier_shouldReturnUpdatedSupplier() throws Exception {
        Supplier supplier = supplierRepository.save(
                createSupplier("Old Supplier Name", "SUP-001")
        );

        String request = """
                {
                    "name": "ABC Electronics Pvt Ltd",
                    "code": "SUP-001",
                    "contactPerson": "Rahul Kumar",
                    "email": "rahul@abcelectronics.com",
                    "phone": "9876543210",
                    "address": "Madhapur",
                    "city": "Hyderabad",
                    "state": "Telangana",
                    "country": "India"
                }
                """;

        mockMvc.perform(put("/api/v1/suppliers/{id}", supplier.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("ABC Electronics Pvt Ltd"))
                .andExpect(jsonPath("$.city").value("Hyderabad"));
    }

    @Test
    void deactivateSupplier_shouldMarkSupplierInactive() throws Exception {
        Supplier supplier = supplierRepository.save(
                createSupplier("ABC Electronics", "SUP-001")
        );

        mockMvc.perform(patch("/api/v1/suppliers/{id}/deactivate", supplier.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/suppliers/{id}", supplier.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void createSupplier_withDuplicateCode_shouldReturnConflict() throws Exception {
        supplierRepository.save(createSupplier("First Supplier", "SUP-001"));

        String request = """
                {
                    "name": "Second Supplier",
                    "code": "SUP-001",
                    "city": "Hyderabad",
                    "country": "India"
                }
                """;

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void createSupplier_withInvalidInput_shouldReturnBadRequest() throws Exception {
        String request = """
                {
                    "name": "",
                    "code": "",
                    "city": "",
                    "country": ""
                }
                """;

        mockMvc.perform(post("/api/v1/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    private Supplier createSupplier(String name, String code) {
        Supplier supplier = new Supplier();
        supplier.setName(name);
        supplier.setCode(code);
        supplier.setContactPerson("Rahul Kumar");
        supplier.setEmail("rahul@abcelectronics.com");
        supplier.setPhone("9876543210");
        supplier.setAddress("Madhapur");
        supplier.setCity("Hyderabad");
        supplier.setState("Telangana");
        supplier.setCountry("India");
        supplier.setActive(true);
        return supplier;
    }
}

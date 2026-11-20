package com.stockflow.controller;
//import org.springframework.security.test.context.support.WithMockUser;
import com.stockflow.entity.Warehouse;
import com.stockflow.repository.WarehouseRepository;
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
class WarehouseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @BeforeEach
    void setUp() {
        warehouseRepository.deleteAll();
    }

    @Test
    void createWarehouse_shouldReturnCreated() throws Exception {
        String request = """
                {
                    "name": "Hyderabad Central Warehouse",
                    "code": "HYD-001",
                    "address": "Gachibowli",
                    "city": "Hyderabad",
                    "state": "Telangana",
                    "country": "India"
                }
                """;

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Hyderabad Central Warehouse"))
                .andExpect(jsonPath("$.code").value("HYD-001"))
                .andExpect(jsonPath("$.city").value("Hyderabad"))
                .andExpect(jsonPath("$.country").value("India"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getAllWarehouses_shouldReturnWarehouseList() throws Exception {
        warehouseRepository.save(createWarehouse("Hyderabad Warehouse", "HYD-001"));
        warehouseRepository.save(createWarehouse("Vizag Warehouse", "VZG-001"));

        mockMvc.perform(get("/api/v1/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getWarehouseById_shouldReturnWarehouse() throws Exception {
        Warehouse warehouse = warehouseRepository.save(
                createWarehouse("Hyderabad Warehouse", "HYD-001")
        );

        mockMvc.perform(get("/api/v1/warehouses/{id}", warehouse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(warehouse.getId()))
                .andExpect(jsonPath("$.name").value("Hyderabad Warehouse"))
                .andExpect(jsonPath("$.code").value("HYD-001"));
    }

    @Test
    void getWarehouseById_whenNotFound_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/warehouses/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWarehouse_shouldReturnUpdatedWarehouse() throws Exception {
        Warehouse warehouse = warehouseRepository.save(
                createWarehouse("Old Warehouse Name", "HYD-001")
        );

        String request = """
                {
                    "name": "Updated Warehouse Name",
                    "code": "HYD-001",
                    "address": "Madhapur",
                    "city": "Hyderabad",
                    "state": "Telangana",
                    "country": "India"
                }
                """;

        mockMvc.perform(put("/api/v1/warehouses/{id}", warehouse.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Warehouse Name"))
                .andExpect(jsonPath("$.address").value("Madhapur"))
                .andExpect(jsonPath("$.city").value("Hyderabad"));
    }

    @Test
    void deactivateWarehouse_shouldMarkWarehouseInactive() throws Exception {
        Warehouse warehouse = warehouseRepository.save(
                createWarehouse("Hyderabad Warehouse", "HYD-001")
        );

        // The controller returns 204 No Content for a successful deactivation.
        mockMvc.perform(patch("/api/v1/warehouses/{id}/deactivate", warehouse.getId()))
                .andExpect(status().isNoContent());

        // A subsequent GET returns 200 OK and confirms the warehouse is inactive.
        mockMvc.perform(get("/api/v1/warehouses/{id}", warehouse.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void createWarehouse_withDuplicateCode_shouldReturnConflict() throws Exception {
        warehouseRepository.save(createWarehouse("First Warehouse", "HYD-001"));

        String request = """
                {
                    "name": "Second Warehouse",
                    "code": "HYD-001",
                    "address": "Madhapur",
                    "city": "Hyderabad",
                    "state": "Telangana",
                    "country": "India"
                }
                """;

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isConflict());
    }

    @Test
    void createWarehouse_withInvalidInput_shouldReturnBadRequest() throws Exception {
        String request = """
                {
                    "name": "",
                    "code": "",
                    "city": "",
                    "country": ""
                }
                """;

        mockMvc.perform(post("/api/v1/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    private Warehouse createWarehouse(String name, String code) {
        return new Warehouse(
                name,
                code,
                "Gachibowli",
                "Hyderabad",
                "Telangana",
                "India"
        );
    }
}

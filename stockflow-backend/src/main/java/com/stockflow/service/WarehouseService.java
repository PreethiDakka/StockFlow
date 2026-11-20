
package com.stockflow.service;

import com.stockflow.dto.WarehouseRequest;
import com.stockflow.dto.WarehouseResponse;
import com.stockflow.entity.Warehouse;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.repository.WarehouseRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository) {
        this.warehouseRepository = warehouseRepository;
    }

    private WarehouseResponse mapToResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getCode(),
                warehouse.getAddress(),
                warehouse.getCity(),
                warehouse.getState(),
                warehouse.getCountry(),
                warehouse.getActive(),
                warehouse.getCreatedAt(),
                warehouse.getUpdatedAt()
        );
    }

    @Transactional
    public WarehouseResponse createWarehouse(WarehouseRequest request) {

        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException(
                    "Warehouse with code " + request.getCode()
                            + " already exists"
            );
        }

        Warehouse warehouse = new Warehouse(
                request.getName(),
                request.getCode(),
                request.getAddress(),
                request.getCity(),
                request.getState(),
                request.getCountry()
        );

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        return mapToResponse(savedWarehouse);
    }

    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Optional<WarehouseResponse> getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .map(this::mapToResponse);
    }

    @Transactional
    public Optional<WarehouseResponse> updateWarehouse(
            Long id, WarehouseRequest request) {

        return warehouseRepository.findById(id).map(existingWarehouse -> {

            if (warehouseRepository.existsByCodeAndIdNot(
                    request.getCode(), id)) {

                throw new DuplicateResourceException(
                        "Warehouse code " + request.getCode()
                                + " is already used by another warehouse"
                );
            }

            existingWarehouse.setName(request.getName());
            existingWarehouse.setCode(request.getCode());
            existingWarehouse.setAddress(request.getAddress());
            existingWarehouse.setCity(request.getCity());
            existingWarehouse.setState(request.getState());
            existingWarehouse.setCountry(request.getCountry());

            Warehouse updatedWarehouse =
                    warehouseRepository.save(existingWarehouse);

            return mapToResponse(updatedWarehouse);
        });
    }

    @Transactional
    public boolean deactivateWarehouse(Long id) {

        Optional<Warehouse> warehouseOptional =
                warehouseRepository.findById(id);

        if (warehouseOptional.isEmpty()) {
            return false;
        }

        Warehouse warehouse = warehouseOptional.get();
        warehouse.setActive(false);

        warehouseRepository.save(warehouse);

        return true;
    }

    @Transactional
    public boolean activateWarehouse(Long id) {

        Optional<Warehouse> warehouseOptional =
                warehouseRepository.findById(id);

        if (warehouseOptional.isEmpty()) {
            return false;
        }

        Warehouse warehouse = warehouseOptional.get();
        warehouse.setActive(true);

        warehouseRepository.save(warehouse);

        return true;
    }
}

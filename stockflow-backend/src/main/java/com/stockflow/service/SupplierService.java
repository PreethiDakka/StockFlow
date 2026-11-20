
package com.stockflow.service;

import com.stockflow.dto.SupplierRequest;
import com.stockflow.dto.SupplierResponse;
import com.stockflow.entity.Supplier;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.repository.SupplierRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public SupplierService(SupplierRepository supplierRepository) {
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {

        if (supplierRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException(
                    "Supplier code already exists: " + request.code()
            );
        }

        Supplier supplier = new Supplier();
        applyRequest(supplier, request);
        supplier.setActive(true);

        return toResponse(supplierRepository.save(supplier));
    }

    public List<SupplierResponse> getAll() {
        return supplierRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public SupplierResponse getById(Long id) {
        return toResponse(findSupplier(id));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {

        Supplier supplier = findSupplier(id);

        if (supplierRepository.existsByCodeAndIdNot(request.code(), id)) {
            throw new DuplicateResourceException(
                    "Supplier code already exists: " + request.code()
            );
        }

        applyRequest(supplier, request);

        return toResponse(supplierRepository.saveAndFlush(supplier));
    }

    @Transactional
    public void deactivate(Long id) {
        Supplier supplier = findSupplier(id);
        supplier.setActive(false);
        supplierRepository.save(supplier);
    }

    @Transactional
    public void activate(Long id) {
        Supplier supplier = findSupplier(id);
        supplier.setActive(true);
        supplierRepository.save(supplier);
    }

    private Supplier findSupplier(Long id) {
        return supplierRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Supplier not found with id: " + id
                ));
    }

    private void applyRequest(
            Supplier supplier,
            SupplierRequest request) {

        supplier.setName(request.name().trim());
        supplier.setCode(request.code().trim());
        supplier.setContactPerson(trimToNull(request.contactPerson()));
        supplier.setEmail(trimToNull(request.email()));
        supplier.setPhone(trimToNull(request.phone()));
        supplier.setAddress(trimToNull(request.address()));
        supplier.setCity(request.city().trim());
        supplier.setState(trimToNull(request.state()));
        supplier.setCountry(request.country().trim());
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    private SupplierResponse toResponse(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getCode(),
                supplier.getContactPerson(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getCity(),
                supplier.getState(),
                supplier.getCountry(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}

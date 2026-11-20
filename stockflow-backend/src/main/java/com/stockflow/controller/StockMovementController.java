package com.stockflow.controller;

import com.stockflow.dto.StockTransferRequest;
import com.stockflow.dto.StockTransferResponse;

import com.stockflow.dto.StockMovementResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

import com.stockflow.dto.StockOutRequest;
import com.stockflow.dto.StockOutResponse;

import com.stockflow.dto.StockInRequest;
import com.stockflow.dto.StockInResponse;
import com.stockflow.service.StockMovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stock-movements")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(
            StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @PostMapping("/stock-in")
    public ResponseEntity<StockInResponse> stockIn(
            @Valid @RequestBody StockInRequest request) {

        StockInResponse response =
                stockMovementService.stockIn(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

@PostMapping("/stock-out")
public ResponseEntity<StockOutResponse> stockOut(
        @Valid @RequestBody StockOutRequest request) {

    StockOutResponse response =
            stockMovementService.stockOut(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}



@GetMapping
public ResponseEntity<Page<StockMovementResponse>> getAllMovements(
        @PageableDefault(
                size = 10,
                sort = "createdAt",
                direction = Sort.Direction.DESC
        ) Pageable pageable) {

    return ResponseEntity.ok(
            stockMovementService.getAllMovements(pageable));
}

@GetMapping("/{id}")
public ResponseEntity<StockMovementResponse> getMovementById(
        @PathVariable Long id) {

    return ResponseEntity.ok(
            stockMovementService.getMovementById(id));
}

@GetMapping("/product/{productId}")
public ResponseEntity<Page<StockMovementResponse>> getMovementsByProduct(
        @PathVariable Long productId,
        @PageableDefault(size = 10) Pageable pageable) {

    return ResponseEntity.ok(
            stockMovementService.getMovementsByProduct(productId, pageable));
}

@GetMapping("/warehouse/{warehouseId}")
public ResponseEntity<Page<StockMovementResponse>> getMovementsByWarehouse(
        @PathVariable Long warehouseId,
        @PageableDefault(size = 10) Pageable pageable) {

    return ResponseEntity.ok(
            stockMovementService.getMovementsByWarehouse(
                    warehouseId, pageable));
}

@GetMapping("/type/{type}")
public ResponseEntity<Page<StockMovementResponse>> getMovementsByType(
        @PathVariable String type,
        @PageableDefault(size = 10) Pageable pageable) {

    return ResponseEntity.ok(
            stockMovementService.getMovementsByType(type, pageable));
}


@PostMapping("/transfer")
public ResponseEntity<StockTransferResponse> transferStock(
        @Valid @RequestBody StockTransferRequest request) {

    StockTransferResponse response =
            stockMovementService.transferStock(request);

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}


}

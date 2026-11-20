
package com.stockflow.service;

import com.stockflow.dto.ProductRequest;
import com.stockflow.dto.ProductResponse;
import com.stockflow.entity.Product;
import com.stockflow.exception.DuplicateResourceException;
import com.stockflow.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    private ProductResponse mapToResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getPrice(),
                product.getReorderLevel()
        );
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException(
                    "Product with SKU " + request.getSku() + " already exists"
            );
        }

        Product product = new Product(
                request.getName(),
                request.getSku(),
                request.getDescription(),
                request.getPrice(),
                request.getReorderLevel()
        );

        Product savedProduct = productRepository.save(product);

        return mapToResponse(savedProduct);
    }

    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllByActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public Optional<ProductResponse> getProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id)
                .map(this::mapToResponse);
    }

    @Transactional
    public Optional<ProductResponse> updateProduct(
            Long id, ProductRequest request) {

        return productRepository.findByIdAndActiveTrue(id)
                .map(existingProduct -> {

                    if (productRepository.existsBySkuAndIdNot(
                            request.getSku(), id)) {

                        throw new DuplicateResourceException(
                                "SKU " + request.getSku()
                                        + " is already used by another product"
                        );
                    }

                    existingProduct.setName(request.getName());
                    existingProduct.setSku(request.getSku());
                    existingProduct.setDescription(request.getDescription());
                    existingProduct.setPrice(request.getPrice());
                    existingProduct.setReorderLevel(request.getReorderLevel());

                    Product updatedProduct =
                            productRepository.save(existingProduct);

                    return mapToResponse(updatedProduct);
                });
    }

    @Transactional
    public boolean deactivateProduct(Long id) {

        Optional<Product> optionalProduct =
                productRepository.findByIdAndActiveTrue(id);

        if (optionalProduct.isEmpty()) {
            return false;
        }

        Product product = optionalProduct.get();
        product.setActive(false);

        productRepository.save(product);

        return true;
    }
}

package com.doceflux.doceflux_api.product.service;

import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.dto.ProductRequest;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProductService {

    private static final int PRICE_SCALE = 2;

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product create(UUID storeId, ProductRequest request) {
        Product product = new Product(
                storeId,
                request.name().trim(),
                request.description(),
                request.price().setScale(PRICE_SCALE),
                request.stockQuantity(),
                request.imageUrl());
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(UUID storeId, Boolean active) {
        if (active == null) {
            return productRepository.findByStoreId(storeId);
        }
        return productRepository.findByStoreIdAndActive(storeId, active);
    }

    @Transactional(readOnly = true)
    public Product findById(UUID id, UUID storeId) {
        return productRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
    }

    @Transactional
    public Product update(UUID id, UUID storeId, ProductRequest request) {
        Product product = findById(id, storeId);
        product.update(
                request.name().trim(),
                request.description(),
                request.price().setScale(PRICE_SCALE),
                request.stockQuantity(),
                request.imageUrl());
        return product;
    }

    @Transactional
    public Product changeActive(UUID id, UUID storeId, boolean active) {
        Product product = findById(id, storeId);
        product.changeActive(active);
        return product;
    }
}

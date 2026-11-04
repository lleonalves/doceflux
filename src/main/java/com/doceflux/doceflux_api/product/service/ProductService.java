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
    public Product create(ProductRequest request) {
        Product product = new Product(
                request.name().trim(),
                request.description(),
                request.price().setScale(PRICE_SCALE));
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<Product> findAll(Boolean active) {
        if (active == null) {
            return productRepository.findAll();
        }
        return productRepository.findByActive(active);
    }

    @Transactional(readOnly = true)
    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + id));
    }

    @Transactional
    public Product update(UUID id, ProductRequest request) {
        Product product = findById(id);
        product.update(
                request.name().trim(),
                request.description(),
                request.price().setScale(PRICE_SCALE));
        return product;
    }

    @Transactional
    public Product changeActive(UUID id, boolean active) {
        Product product = findById(id);
        product.changeActive(active);
        return product;
    }
}

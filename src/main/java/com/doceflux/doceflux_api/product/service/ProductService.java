package com.doceflux.doceflux_api.product.service;

import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.dto.ProductRequest;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    public List<Product> findAll() {
        return productRepository.findAll();
    }
}

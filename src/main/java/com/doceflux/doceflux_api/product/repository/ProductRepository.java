package com.doceflux.doceflux_api.product.repository;

import com.doceflux.doceflux_api.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    long countByActiveTrue();
}

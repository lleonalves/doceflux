package com.doceflux.doceflux_api.product.repository;

import com.doceflux.doceflux_api.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByActive(boolean active);

    long countByActiveTrue();
}

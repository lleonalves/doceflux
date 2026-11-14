package com.doceflux.doceflux_api.product.repository;

import com.doceflux.doceflux_api.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByStoreId(UUID storeId);

    List<Product> findByStoreIdAndActive(UUID storeId, boolean active);

    Optional<Product> findByIdAndStoreId(UUID id, UUID storeId);

    long countByStoreIdAndActiveTrue(UUID storeId);
}

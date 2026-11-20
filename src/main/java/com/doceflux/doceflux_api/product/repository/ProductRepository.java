package com.doceflux.doceflux_api.product.repository;

import com.doceflux.doceflux_api.product.domain.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<Product> findByStoreId(UUID storeId);

    List<Product> findByStoreIdAndActive(UUID storeId, boolean active);

    Optional<Product> findByIdAndStoreId(UUID id, UUID storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id and p.storeId = :storeId")
    Optional<Product> findByIdAndStoreIdForUpdate(@Param("id") UUID id, @Param("storeId") UUID storeId);

    long countByStoreIdAndActiveTrue(UUID storeId);
}

package com.doceflux.doceflux_api.auth.repository;

import com.doceflux.doceflux_api.auth.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {
}

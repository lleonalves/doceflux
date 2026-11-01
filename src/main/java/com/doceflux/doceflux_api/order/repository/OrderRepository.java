package com.doceflux.doceflux_api.order.repository;

import com.doceflux.doceflux_api.order.domain.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Override
    @EntityGraph(attributePaths = {"customer", "items"})
    List<Order> findAll();

    @Override
    @EntityGraph(attributePaths = {"customer", "items"})
    Optional<Order> findById(UUID id);
}

package com.doceflux.doceflux_api.order.repository;

import com.doceflux.doceflux_api.order.domain.Order;
import org.springframework.data.jpa.repository.EntityGraph;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @EntityGraph(attributePaths = {"customer", "items"})
    List<Order> findAllByStoreId(UUID storeId);

    @EntityGraph(attributePaths = {"customer", "items"})
    Optional<Order> findByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByIdAndStoreId(UUID id, UUID storeId);

    boolean existsByCustomerIdAndStoreId(UUID customerId, UUID storeId);

    @Query("""
            select o.status as status, count(o) as count, coalesce(sum(o.totalAmount), 0) as totalAmount
            from Order o
            where o.storeId = :storeId and o.createdAt >= :from and o.createdAt < :to
            group by o.status
            """)
    List<OrderStatusTotals> summarizeByStatus(@Param("storeId") UUID storeId, @Param("from") Instant from,
                                           @Param("to") Instant to);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id and o.storeId = :storeId")
    Optional<Order> findByIdAndStoreIdForUpdate(@Param("id") UUID id, @Param("storeId") UUID storeId);
}

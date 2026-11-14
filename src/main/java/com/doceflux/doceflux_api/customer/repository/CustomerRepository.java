package com.doceflux.doceflux_api.customer.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query(value = """
            select * from customers c
            where c.store_id = :storeId
              and ((:name <> '' and strpos(lower(c.name), lower(:name)) > 0)
               or (:digits <> '' and strpos(regexp_replace(c.phone, '\\D', '', 'g'), :digits) > 0))
            order by c.name, c.created_at
            """, nativeQuery = true)
    List<Customer> searchByNameOrPhone(@Param("storeId") UUID storeId, @Param("name") String name,
                                       @Param("digits") String digits);

    List<Customer> findAllByStoreId(UUID storeId);

    Optional<Customer> findByIdAndStoreId(UUID id, UUID storeId);

    long countByStoreId(UUID storeId);
}

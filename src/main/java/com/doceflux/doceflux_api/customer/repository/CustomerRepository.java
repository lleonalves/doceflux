package com.doceflux.doceflux_api.customer.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    @Query(value = """
            select * from customers c
            where (:name <> '' and strpos(lower(c.name), lower(:name)) > 0)
               or (:digits <> '' and strpos(regexp_replace(c.phone, '\\D', '', 'g'), :digits) > 0)
            order by c.name, c.created_at
            """, nativeQuery = true)
    List<Customer> searchByNameOrPhone(@Param("name") String name, @Param("digits") String digits);
}

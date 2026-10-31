package com.doceflux.doceflux_api.customer.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}

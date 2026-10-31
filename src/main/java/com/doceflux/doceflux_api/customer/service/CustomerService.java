package com.doceflux.doceflux_api.customer.service;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.dto.CustomerRequest;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public Customer create(CustomerRequest request) {
        Customer customer = new Customer(
                request.name().trim(),
                request.phone().trim(),
                request.email(),
                request.address());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer findById(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
    }
}

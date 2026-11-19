package com.doceflux.doceflux_api.customer.service;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.dto.CustomerRequest;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    public CustomerService(CustomerRepository customerRepository, OrderRepository orderRepository) {
        this.customerRepository = customerRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Customer create(UUID storeId, CustomerRequest request) {
        Customer customer = new Customer(
                storeId,
                request.name().trim(),
                request.phone().trim(),
                request.email(),
                request.address());
        return customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public List<Customer> findAll(UUID storeId) {
        return customerRepository.findAllByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public List<Customer> search(UUID storeId, String search) {
        String term = search == null ? "" : search.trim();
        if (term.isEmpty()) {
            return findAll(storeId);
        }
        String digits = term.replaceAll("\\D", "");
        return customerRepository.searchByNameOrPhone(storeId, term, digits);
    }

    @Transactional
    public Customer update(UUID id, UUID storeId, CustomerRequest request) {
        Customer customer = findById(id, storeId);
        customer.update(
                request.name().trim(),
                request.phone().trim(),
                request.email(),
                request.address());
        return customer;
    }

    @Transactional
    public void delete(UUID id, UUID storeId) {
        Customer customer = findById(id, storeId);
        if (orderRepository.existsByCustomerIdAndStoreId(id, storeId)) {
            throw new BusinessRuleException("Não é possível excluir um cliente que já possui pedidos registrados.");
        }
        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public Customer findById(UUID id, UUID storeId) {
        return customerRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + id));
    }
}

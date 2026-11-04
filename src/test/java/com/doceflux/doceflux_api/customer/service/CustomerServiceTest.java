package com.doceflux.doceflux_api.customer.service;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.dto.CustomerRequest;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createsCustomerWithGeneratedIdAndCreationDate() {
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer created = customerService.create(
                new CustomerRequest("Maria", "11999999999", "maria@email.com", "Rua A, 10"));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getName()).isEqualTo("Maria");
        assertThat(created.getPhone()).isEqualTo("11999999999");
        assertThat(created.getEmail()).isEqualTo("maria@email.com");
        assertThat(created.getAddress()).isEqualTo("Rua A, 10");
    }

    @Test
    void findsExistingCustomerById() {
        Customer customer = new Customer("Maria", "11999999999", null, null);
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThat(customerService.findById(customer.getId())).isSameAs(customer);
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsAllCustomers() {
        List<Customer> customers = List.of(
                new Customer("Maria", "11999999999", null, null),
                new Customer("Ana", "11888888888", null, null));
        when(customerRepository.findAll()).thenReturn(customers);

        assertThat(customerService.findAll()).containsExactlyElementsOf(customers);
    }

    @Test
    void updatesAllEditableFieldsTrimmingNameAndPhone() {
        Customer customer = new Customer("Maria", "11999999999", "maria@email.com", "Rua A, 10");
        UUID id = customer.getId();
        Instant createdAt = customer.getCreatedAt();
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));

        Customer updated = customerService.update(id,
                new CustomerRequest("  Maria Oliveira ", " (12) 99999-9999  ", null, null));

        assertThat(updated.getId()).isEqualTo(id);
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getName()).isEqualTo("Maria Oliveira");
        assertThat(updated.getPhone()).isEqualTo("(12) 99999-9999");
        assertThat(updated.getEmail()).isNull();
        assertThat(updated.getAddress()).isNull();
        verify(customerRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenCustomerDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(id,
                new CustomerRequest("Maria", "11999999999", null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void searchWithoutTermListsAllCustomers() {
        List<Customer> customers = List.of(new Customer("Maria", "11999999999", null, null));
        when(customerRepository.findAll()).thenReturn(customers);

        assertThat(customerService.search(null)).isEqualTo(customers);
        assertThat(customerService.search("")).isEqualTo(customers);
        assertThat(customerService.search("   ")).isEqualTo(customers);
        verify(customerRepository, never()).searchByNameOrPhone(any(), any());
    }

    @Test
    void searchTrimsTermAndExtractsPhoneDigits() {
        List<Customer> customers = List.of(new Customer("Maria", "(12) 99999-9999", null, null));
        when(customerRepository.searchByNameOrPhone("(12) 99999-9999", "12999999999")).thenReturn(customers);

        assertThat(customerService.search("  (12) 99999-9999 ")).isEqualTo(customers);
    }

    @Test
    void searchWithoutDigitsSendsEmptyDigits() {
        when(customerRepository.searchByNameOrPhone("Maria", "")).thenReturn(List.of());

        assertThat(customerService.search(" Maria ")).isEmpty();
        verify(customerRepository).searchByNameOrPhone("Maria", "");
    }
}

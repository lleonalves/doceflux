package com.doceflux.doceflux_api.customer.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void persistsAndRetrievesCustomerWithOptionalFieldsEmpty() {
        Customer saved = customerRepository.saveAndFlush(new Customer("Maria", "11999999999", null, null));

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Maria");
        assertThat(found.getEmail()).isNull();
        assertThat(found.getAddress()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
    }
}

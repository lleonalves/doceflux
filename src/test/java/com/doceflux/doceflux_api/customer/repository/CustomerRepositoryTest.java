package com.doceflux.doceflux_api.customer.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class CustomerRepositoryTest {

    private static final UUID STORE = UUID.randomUUID();

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void persistsAndRetrievesCustomerWithOptionalFieldsEmpty() {
        Customer saved = customerRepository.saveAndFlush(new Customer(STORE, "Maria", "11999999999", null, null));

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Maria");
        assertThat(found.getEmail()).isNull();
        assertThat(found.getAddress()).isNull();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void updatePersistsChangesKeepingIdCreationDateAndRowCount() {
        Customer saved = customerRepository.saveAndFlush(
                new Customer(STORE, "Zelda Antiga", "(77) 91111-2222", "z@email.com", "Rua Z"));
        entityManager.clear();
        long countBefore = customerRepository.count();

        Customer loaded = customerRepository.findById(saved.getId()).orElseThrow();
        Instant persistedCreatedAt = loaded.getCreatedAt();
        loaded.update("Zelda Nova", "(77) 93333-4444", null, null);
        customerRepository.flush();
        entityManager.clear();

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Zelda Nova");
        assertThat(found.getPhone()).isEqualTo("(77) 93333-4444");
        assertThat(found.getEmail()).isNull();
        assertThat(found.getAddress()).isNull();
        assertThat(found.getCreatedAt()).isEqualTo(persistedCreatedAt);
        assertThat(customerRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void allowsDifferentCustomersWithSamePhone() {
        customerRepository.saveAndFlush(new Customer(STORE, "Zora Um", "(77) 95555-6666", null, null));
        customerRepository.saveAndFlush(new Customer(STORE, "Zora Dois", "(77) 95555-6666", null, null));

        assertThat(customerRepository.searchByNameOrPhone(STORE, "", "77955556666"))
                .extracting(Customer::getName)
                .containsExactly("Zora Dois", "Zora Um");
    }

    @Test
    void searchesByNameIgnoringCaseAndPartially() {
        customerRepository.saveAndFlush(new Customer(STORE, "Quiteria Oliveira", "(77) 97777-0001", null, null));
        customerRepository.saveAndFlush(new Customer(STORE, "Outra Pessoa", "(77) 97777-0002", null, null));

        assertThat(customerRepository.searchByNameOrPhone(STORE, "Quiteria Oliveira", ""))
                .extracting(Customer::getName).containsExactly("Quiteria Oliveira");
        assertThat(customerRepository.searchByNameOrPhone(STORE, "uITER", ""))
                .extracting(Customer::getName).containsExactly("Quiteria Oliveira");
        assertThat(customerRepository.searchByNameOrPhone(STORE, "QUITERIA", ""))
                .extracting(Customer::getName).containsExactly("Quiteria Oliveira");
    }

    @Test
    void searchesByPhoneIgnoringMask() {
        customerRepository.saveAndFlush(new Customer(STORE, "Wanda Mascara", "(77) 98888-1234", null, null));

        assertThat(customerRepository.searchByNameOrPhone(STORE, "", "77988881234")).hasSize(1);
        assertThat(customerRepository.searchByNameOrPhone(STORE, "", "88881")).hasSize(1);
        assertThat(customerRepository.searchByNameOrPhone(STORE, "", "77988881")).hasSize(1);
    }

    @Test
    void searchMatchesNameOrPhone() {
        customerRepository.saveAndFlush(new Customer(STORE, "Xuxa Nome", "(77) 90000-0001", null, null));
        customerRepository.saveAndFlush(new Customer(STORE, "Yara Fone", "(77) 90000-9999", null, null));

        assertThat(customerRepository.searchByNameOrPhone(STORE, "Xuxa", "0000999"))
                .extracting(Customer::getName).containsExactlyInAnyOrder("Xuxa Nome", "Yara Fone");
    }

    @Test
    void searchWithoutDigitsDoesNotMatchEveryPhone() {
        customerRepository.saveAndFlush(new Customer(STORE, "Vera Sem Match", "(77) 92222-0000", null, null));

        assertThat(customerRepository.searchByNameOrPhone(STORE, "nome-inexistente-xyz", "")).isEmpty();
    }

    @Test
    void searchTreatsWildcardCharactersLiterally() {
        customerRepository.saveAndFlush(new Customer(STORE, "Tereza Coringa", "(77) 94444-0000", null, null));

        assertThat(customerRepository.searchByNameOrPhone(STORE, "%", "")).isEmpty();
        assertThat(customerRepository.searchByNameOrPhone(STORE, "_", "")).isEmpty();
    }
}

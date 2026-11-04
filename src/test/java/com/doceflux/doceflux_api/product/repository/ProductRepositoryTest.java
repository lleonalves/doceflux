package com.doceflux.doceflux_api.product.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.order.domain.Order;
import com.doceflux.doceflux_api.order.domain.OrderItem;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.product.domain.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void persistsAndRetrievesProductWithExactPrice() {
        Product saved = productRepository.saveAndFlush(new Product("Bolo", null, new BigDecimal("45.50")));

        Product found = productRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getName()).isEqualTo("Bolo");
        assertThat(found.getDescription()).isNull();
        assertThat(found.getPrice()).isEqualByComparingTo("45.50");
        assertThat(found.isActive()).isTrue();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNotNull();
    }

    @Test
    void filtersProductsByActiveStatus() {
        Product active = productRepository.save(new Product("Filtro Ativo", null, new BigDecimal("10.00")));
        Product inactive = productRepository.save(new Product("Filtro Inativo", null, new BigDecimal("10.00")));
        inactive.changeActive(false);
        productRepository.flush();
        entityManager.clear();

        assertThat(productRepository.findByActive(true)).extracting(Product::getId)
                .contains(active.getId()).doesNotContain(inactive.getId());
        assertThat(productRepository.findByActive(false)).extracting(Product::getId)
                .contains(inactive.getId()).doesNotContain(active.getId());
    }

    @Test
    void persistsUpdateAndActiveChangeKeepingCreationDate() {
        Product saved = productRepository.saveAndFlush(new Product("Antigo", "Desc", new BigDecimal("10.00")));
        entityManager.clear();
        Product loaded = productRepository.findById(saved.getId()).orElseThrow();
        Instant createdAt = loaded.getCreatedAt();

        loaded.update("Novo", null, new BigDecimal("12.50"));
        loaded.changeActive(false);
        productRepository.flush();
        entityManager.clear();

        Product found = productRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Novo");
        assertThat(found.getDescription()).isNull();
        assertThat(found.getPrice()).isEqualByComparingTo("12.50");
        assertThat(found.isActive()).isFalse();
        assertThat(found.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void editingOrDeactivatingProductDoesNotChangeExistingOrderSnapshots() {
        Product product = productRepository.save(new Product("Bolo Original", null, new BigDecimal("40.00")));
        Customer customer = customerRepository.save(new Customer("Maria", "11999999999", null, null));
        Order order = new Order(customer, "Pedido");
        order.addItem(OrderItem.fromCatalog(product, null, 2));
        orderRepository.saveAndFlush(order);
        entityManager.clear();

        Product loaded = productRepository.findById(product.getId()).orElseThrow();
        loaded.update("Bolo Renomeado", null, new BigDecimal("99.00"));
        loaded.changeActive(false);
        productRepository.flush();
        entityManager.clear();

        Order reloaded = orderRepository.findById(order.getId()).orElseThrow();
        OrderItem item = reloaded.getItems().getFirst();
        assertThat(item.getProductNameSnapshot()).isEqualTo("Bolo Original");
        assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("40.00");
        assertThat(item.getSubtotal()).isEqualByComparingTo("80.00");
        assertThat(reloaded.getTotalAmount()).isEqualByComparingTo("80.00");
        assertThat(item.getProduct().getId()).isEqualTo(product.getId());
    }
}

package com.doceflux.doceflux_api.order.repository;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.order.domain.Order;
import com.doceflux.doceflux_api.order.domain.OrderItem;
import com.doceflux.doceflux_api.order.domain.OrderItemType;
import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private Order persistOrderWithCatalogAndCustomItems(Product product) {
        Customer customer = customerRepository.save(new Customer("Maria", "11999999999", null, null));
        Order order = new Order(customer, "Retirada sábado");
        order.addItem(OrderItem.fromCatalog(product, "Sem granulado", 3));
        order.addItem(OrderItem.custom("Bolo temático", "Unicórnio", 2, new BigDecimal("120.50")));
        orderRepository.saveAndFlush(order);
        entityManager.clear();
        return order;
    }

    @Test
    void persistsOrderWithItemsAndRelationships() {
        Product product = productRepository.save(new Product("Bolo de Cenoura", null, new BigDecimal("45.50")));
        Order saved = persistOrderWithCatalogAndCustomItems(product);

        Order found = orderRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getCustomer().getName()).isEqualTo("Maria");
        assertThat(found.getDescription()).isEqualTo("Retirada sábado");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getItems()).hasSize(2);

        OrderItem catalog = found.getItems().stream()
                .filter(i -> i.getItemType() == OrderItemType.CATALOG).findFirst().orElseThrow();
        assertThat(catalog.getProduct().getId()).isEqualTo(product.getId());
        assertThat(catalog.getSubtotal()).isEqualByComparingTo("136.50");

        OrderItem custom = found.getItems().stream()
                .filter(i -> i.getItemType() == OrderItemType.CUSTOM).findFirst().orElseThrow();
        assertThat(custom.getProduct()).isNull();
        assertThat(custom.getProductNameSnapshot()).isEqualTo("Bolo temático");
        assertThat(custom.getSubtotal()).isEqualByComparingTo("241.00");
    }

    @Test
    void keepsSnapshotsWhenProductIsChangedLater() {
        Product product = productRepository.save(new Product("Bolo de Cenoura", null, new BigDecimal("45.50")));
        Order saved = persistOrderWithCatalogAndCustomItems(product);

        entityManager.createNativeQuery("update products set name = 'Bolo Novo', price = 99.99 where id = :id")
                .setParameter("id", product.getId())
                .executeUpdate();
        entityManager.clear();

        OrderItem catalog = orderRepository.findById(saved.getId()).orElseThrow().getItems().stream()
                .filter(i -> i.getItemType() == OrderItemType.CATALOG).findFirst().orElseThrow();
        assertThat(catalog.getProductNameSnapshot()).isEqualTo("Bolo de Cenoura");
        assertThat(catalog.getUnitPriceSnapshot()).isEqualByComparingTo("45.50");
        assertThat(catalog.getSubtotal()).isEqualByComparingTo("136.50");
        assertThat(catalog.getProduct().getPrice()).isEqualByComparingTo("99.99");
    }

    @Test
    void databaseRejectsCatalogItemWithoutProduct() {
        Customer customer = customerRepository.saveAndFlush(new Customer("Maria", "11999999999", null, null));
        Order order = orderRepository.saveAndFlush(new Order(customer, null));

        assertThatThrownBy(() -> entityManager.createNativeQuery("""
                insert into order_items (id, order_id, item_type, product_name_snapshot, quantity,
                                         unit_price_snapshot, subtotal)
                values (:id, :orderId, 'CATALOG', 'Bolo', 1, 10.00, 10.00)
                """)
                .setParameter("id", UUID.randomUUID())
                .setParameter("orderId", order.getId())
                .executeUpdate())
                .hasRootCauseInstanceOf(java.sql.SQLException.class);
    }
}

package com.doceflux.doceflux_api.order.service;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.order.domain.Order;
import com.doceflux.doceflux_api.order.domain.OrderItem;
import com.doceflux.doceflux_api.order.domain.OrderItemType;
import com.doceflux.doceflux_api.order.domain.OrderStatus;
import com.doceflux.doceflux_api.order.dto.OrderItemRequest;
import com.doceflux.doceflux_api.order.dto.OrderRequest;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
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
class OrderServiceTest {

    private static final UUID STORE = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private final Customer customer = new Customer(STORE, "Maria", "11999999999", null, null);

    private void mockSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsOrderWithCatalogItemSnapshottingNameAndPrice() {
        Product product = new Product(UUID.randomUUID(), "Bolo de Cenoura", null, new BigDecimal("45.50"), 10, null);
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdForUpdate(product.getId(), STORE)).thenReturn(Optional.of(product));
        mockSave();

        Order order = orderService.create(STORE, new OrderRequest(customer.getId(), "Retirada sábado",
                List.of(new OrderItemRequest(product.getId(), null, "Sem granulado", 3, null))));

        assertThat(order.getId()).isNotNull();
        assertThat(order.getCustomer()).isSameAs(customer);
        assertThat(order.getDescription()).isEqualTo("Retirada sábado");
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getItems()).hasSize(1);
        OrderItem item = order.getItems().getFirst();
        assertThat(item.getItemType()).isEqualTo(OrderItemType.CATALOG);
        assertThat(item.getProduct()).isSameAs(product);
        assertThat(item.getProductNameSnapshot()).isEqualTo("Bolo de Cenoura");
        assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("45.50");
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(item.getSubtotal()).isEqualByComparingTo("136.50");
        assertThat(item.getSubtotal().scale()).isEqualTo(2);
    }

    @Test
    void createsOrderWithCustomItemWithoutProduct() {
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));
        mockSave();

        Order order = orderService.create(STORE, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(null, " Bolo temático ", "Tema unicórnio", 2, new BigDecimal("120.5")))));

        OrderItem item = order.getItems().getFirst();
        assertThat(item.getItemType()).isEqualTo(OrderItemType.CUSTOM);
        assertThat(item.getProduct()).isNull();
        assertThat(item.getProductNameSnapshot()).isEqualTo("Bolo temático");
        assertThat(item.getUnitPriceSnapshot()).isEqualByComparingTo("120.50");
        assertThat(item.getSubtotal()).isEqualByComparingTo("241.00");
    }

    @Test
    void rejectsUnknownCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findByIdAndStoreId(id, STORE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(STORE, new OrderRequest(id, null,
                List.of(new OrderItemRequest(null, "Bolo", null, 1, BigDecimal.TEN)))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsCustomerFromAnotherStore() {
        UUID otherStore = UUID.randomUUID();
        when(customerRepository.findByIdAndStoreId(customer.getId(), otherStore)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(otherStore, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(null, "Bolo", null, 1, BigDecimal.TEN)))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsProductFromAnotherStore() {
        UUID productId = UUID.randomUUID();
        UUID otherStore = UUID.randomUUID();
        when(customerRepository.findByIdAndStoreId(customer.getId(), otherStore)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdForUpdate(productId, otherStore)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(otherStore, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(productId, null, null, 1, null)))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository).findByIdAndStoreIdForUpdate(productId, otherStore);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownProduct() {
        UUID productId = UUID.randomUUID();
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdForUpdate(productId, STORE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(STORE, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(productId, null, null, 1, null)))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsInactiveCatalogProduct() {
        Product product = new Product(UUID.randomUUID(), "Bolo de Cenoura", null, new BigDecimal("45.50"), 10, null);
        product.changeActive(false);
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdForUpdate(product.getId(), STORE)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.create(STORE, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(product.getId(), null, null, 1, null)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsCustomItemWithoutNameOrPrice() {
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> orderService.create(STORE, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(null, "  ", null, 1, BigDecimal.TEN)))))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> orderService.create(STORE, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(null, "Bolo", null, 1, null)))))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsCatalogItemWithOverriddenPrice() {
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> orderService.create(STORE, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(UUID.randomUUID(), null, null, 1, BigDecimal.ONE)))))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createsOrderWithPendingStatusAndTotalFromAllItems() {
        Product product = new Product(UUID.randomUUID(), "Bolo de Cenoura", null, new BigDecimal("45.50"), 10, null);
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdForUpdate(product.getId(), STORE)).thenReturn(Optional.of(product));
        mockSave();

        Order order = orderService.create(STORE, new OrderRequest(customer.getId(), null, List.of(
                new OrderItemRequest(product.getId(), null, null, 3, null),
                new OrderItemRequest(null, "Docinhos", null, 2, new BigDecimal("120.50")))));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("377.50");
        assertThat(order.getTotalAmount().scale()).isEqualTo(2);
    }

    private Product stockedProduct(int stock) {
        return new Product(STORE, "Bolo de Cenoura", null, new BigDecimal("45.50"), stock, null);
    }

    private Order orderWithCatalogItem(Product product, int quantity) {
        Order order = new Order(STORE, customer, null);
        order.addItem(OrderItem.fromCatalog(product, null, quantity));
        return order;
    }

    @Test
    void createDecreasesStockOfCatalogItemsOnly() {
        Product product = stockedProduct(10);
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdForUpdate(product.getId(), STORE)).thenReturn(Optional.of(product));
        mockSave();

        orderService.create(STORE, new OrderRequest(customer.getId(), null, List.of(
                new OrderItemRequest(product.getId(), null, null, 3, null),
                new OrderItemRequest(null, "Docinhos", null, 2, BigDecimal.TEN))));

        assertThat(product.getStockQuantity()).isEqualTo(7);
    }

    @Test
    void createRejectsQuantityAboveStockWithoutChangingBalance() {
        Product product = stockedProduct(2);
        when(customerRepository.findByIdAndStoreId(customer.getId(), STORE)).thenReturn(Optional.of(customer));
        when(productRepository.findByIdAndStoreIdForUpdate(product.getId(), STORE)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.create(STORE, new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(product.getId(), null, null, 3, null)))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Estoque insuficiente para o produto: Bolo de Cenoura (disponível: 2)");

        assertThat(product.getStockQuantity()).isEqualTo(2);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void cancelRestoresStockFromPendingAndInProduction() {
        for (OrderStatus from : List.of(OrderStatus.PENDING, OrderStatus.IN_PRODUCTION)) {
            Product product = stockedProduct(7);
            Order order = orderWithCatalogItem(product, 3);
            if (from == OrderStatus.IN_PRODUCTION) {
                order.changeStatus(OrderStatus.IN_PRODUCTION);
            }
            when(orderRepository.findByIdAndStoreIdForUpdate(order.getId(), STORE)).thenReturn(Optional.of(order));
            when(productRepository.findByIdAndStoreIdForUpdate(product.getId(), STORE)).thenReturn(Optional.of(product));

            orderService.changeStatus(order.getId(), STORE, OrderStatus.CANCELLED);

            assertThat(product.getStockQuantity()).isEqualTo(10);
        }
    }

    @Test
    void completingOrderDoesNotTouchStock() {
        Product product = stockedProduct(7);
        Order order = orderWithCatalogItem(product, 3);
        order.changeStatus(OrderStatus.IN_PRODUCTION);
        when(orderRepository.findByIdAndStoreIdForUpdate(order.getId(), STORE)).thenReturn(Optional.of(order));

        orderService.changeStatus(order.getId(), STORE, OrderStatus.COMPLETED);

        assertThat(product.getStockQuantity()).isEqualTo(7);
        verify(productRepository, never()).findByIdAndStoreIdForUpdate(any(), any());
    }

    @Test
    void changesStatusOfExistingOrder() {
        Order order = new Order(STORE, customer, null);
        when(orderRepository.findByIdAndStoreIdForUpdate(order.getId(), STORE)).thenReturn(Optional.of(order));

        Order result = orderService.changeStatus(order.getId(), STORE, OrderStatus.IN_PRODUCTION);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Order order = new Order(STORE, customer, null);
        when(orderRepository.findByIdAndStoreIdForUpdate(order.getId(), STORE)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(order.getId(), STORE, OrderStatus.COMPLETED))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void throwsWhenChangingStatusOfUnknownOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreIdForUpdate(id, STORE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.changeStatus(id, STORE, OrderStatus.CANCELLED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(id, STORE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(id, STORE)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsAllOrders() {
        List<Order> orders = List.of(new Order(STORE, customer, null), new Order(STORE, customer, "x"));
        when(orderRepository.findAllByStoreId(STORE)).thenReturn(orders);

        assertThat(orderService.findAll(STORE)).containsExactlyElementsOf(orders);
    }
}

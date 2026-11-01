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

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderService orderService;

    private final Customer customer = new Customer("Maria", "11999999999", null, null);

    private void mockSave() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createsOrderWithCatalogItemSnapshottingNameAndPrice() {
        Product product = new Product("Bolo de Cenoura", null, new BigDecimal("45.50"));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        mockSave();

        Order order = orderService.create(new OrderRequest(customer.getId(), "Retirada sábado",
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
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        mockSave();

        Order order = orderService.create(new OrderRequest(customer.getId(), null,
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
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(new OrderRequest(id, null,
                List.of(new OrderItemRequest(null, "Bolo", null, 1, BigDecimal.TEN)))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsUnknownProduct() {
        UUID productId = UUID.randomUUID();
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.create(new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(productId, null, null, 1, null)))))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsCustomItemWithoutNameOrPrice() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> orderService.create(new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(null, "  ", null, 1, BigDecimal.TEN)))))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> orderService.create(new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(null, "Bolo", null, 1, null)))))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void rejectsCatalogItemWithOverriddenPrice() {
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> orderService.create(new OrderRequest(customer.getId(), null,
                List.of(new OrderItemRequest(UUID.randomUUID(), null, null, 1, BigDecimal.ONE)))))
                .isInstanceOf(BusinessRuleException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void createsOrderWithPendingStatusAndTotalFromAllItems() {
        Product product = new Product("Bolo de Cenoura", null, new BigDecimal("45.50"));
        when(customerRepository.findById(customer.getId())).thenReturn(Optional.of(customer));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        mockSave();

        Order order = orderService.create(new OrderRequest(customer.getId(), null, List.of(
                new OrderItemRequest(product.getId(), null, null, 3, null),
                new OrderItemRequest(null, "Docinhos", null, 2, new BigDecimal("120.50")))));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getTotalAmount()).isEqualByComparingTo("377.50");
        assertThat(order.getTotalAmount().scale()).isEqualTo(2);
    }

    @Test
    void changesStatusOfExistingOrder() {
        Order order = new Order(customer, null);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        Order result = orderService.changeStatus(order.getId(), OrderStatus.IN_PRODUCTION);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_PRODUCTION);
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Order order = new Order(customer, null);
        when(orderRepository.findByIdForUpdate(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeStatus(order.getId(), OrderStatus.COMPLETED))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void throwsWhenChangingStatusOfUnknownOrder() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.changeStatus(id, OrderStatus.CANCELLED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void throwsWhenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listsAllOrders() {
        List<Order> orders = List.of(new Order(customer, null), new Order(customer, "x"));
        when(orderRepository.findAll()).thenReturn(orders);

        assertThat(orderService.findAll()).containsExactlyElementsOf(orders);
    }
}

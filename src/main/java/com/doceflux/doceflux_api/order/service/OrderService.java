package com.doceflux.doceflux_api.order.service;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.repository.CustomerRepository;
import com.doceflux.doceflux_api.order.domain.Order;
import com.doceflux.doceflux_api.order.domain.OrderItem;
import com.doceflux.doceflux_api.order.domain.OrderStatus;
import com.doceflux.doceflux_api.order.dto.OrderItemRequest;
import com.doceflux.doceflux_api.order.dto.OrderRequest;
import com.doceflux.doceflux_api.order.repository.OrderRepository;
import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        CustomerRepository customerRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public Order create(OrderRequest request) {
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado: " + request.customerId()));

        Order order = new Order(customer, request.description());
        request.items().forEach(item -> order.addItem(toOrderItem(item)));
        return orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order findById(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
    }

    @Transactional
    public Order changeStatus(UUID id, OrderStatus status) {
        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado: " + id));
        order.changeStatus(status);
        return order;
    }

    private OrderItem toOrderItem(OrderItemRequest item) {
        if (item.productId() != null) {
            return catalogItem(item);
        }
        return customItem(item);
    }

    private OrderItem catalogItem(OrderItemRequest item) {
        if (item.name() != null || item.unitPrice() != null) {
            throw new BusinessRuleException("Item de catálogo não aceita nome ou preço unitário informados");
        }
        Product product = productRepository.findById(item.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Produto não encontrado: " + item.productId()));
        if (!product.isActive()) {
            throw new BusinessRuleException(
                    "O produto '" + product.getName() + "' está inativo e não pode ser incluído em novos pedidos");
        }
        return OrderItem.fromCatalog(product,item.description(), item.quantity());
    }

    private OrderItem customItem(OrderItemRequest item) {
        if (item.name() == null || item.name().isBlank()) {
            throw new BusinessRuleException("O nome é obrigatório para itens personalizados");
        }
        if (item.unitPrice() == null) {
            throw new BusinessRuleException("O preço unitário é obrigatório para itens personalizados");
        }
        return OrderItem.custom(item.name().trim(), item.description(), item.quantity(), item.unitPrice());
    }
}

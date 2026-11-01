package com.doceflux.doceflux_api.order.controller;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.order.domain.Order;
import com.doceflux.doceflux_api.order.domain.OrderItem;
import com.doceflux.doceflux_api.order.domain.OrderStatus;
import com.doceflux.doceflux_api.order.service.OrderService;
import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final String CUSTOMER_ID = "11111111-1111-1111-1111-111111111111";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private ResultActions postOrder(String json) throws Exception {
        return mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void assertRejected(String json, String field, String message) throws Exception {
        postOrder(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['" + field + "']").value(message));
        verify(orderService, never()).create(any());
    }

    private Order sampleOrder() {
        Order order = new Order(new Customer("Maria", "11999999999", null, null), "Retirada sábado");
        order.addItem(OrderItem.fromCatalog(new Product("Bolo", null, new BigDecimal("45.50")), null, 2));
        order.addItem(OrderItem.custom("Docinhos", "Cento", 1, new BigDecimal("80.00")));
        return order;
    }

    @Test
    void createsOrder() throws Exception {
        Order order = sampleOrder();
        when(orderService.create(any())).thenReturn(order);

        postOrder("""
                {"customerId":"%s","items":[{"name":"Docinhos","quantity":1,"unitPrice":80.00}]}
                """.formatted(CUSTOMER_ID))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/orders/" + order.getId()))
                .andExpect(jsonPath("$.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.customerName").value("Maria"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].itemType").value("CATALOG"))
                .andExpect(jsonPath("$.items[0].name").value("Bolo"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(45.50))
                .andExpect(jsonPath("$.items[0].subtotal").value(91.00))
                .andExpect(jsonPath("$.items[1].itemType").value("CUSTOM"))
                .andExpect(jsonPath("$.items[1].productId").doesNotExist());
    }

    @Test
    void rejectsOrderWithoutCustomer() throws Exception {
        assertRejected("""
                {"items":[{"name":"Bolo","quantity":1,"unitPrice":10}]}
                """, "customerId", "O cliente é obrigatório");
    }

    @Test
    void rejectsOrderWithoutItems() throws Exception {
        assertRejected("""
                {"customerId":"%s","items":[]}
                """.formatted(CUSTOMER_ID), "items", "O pedido deve ter ao menos um item");
    }

    @Test
    void rejectsItemWithoutQuantity() throws Exception {
        assertRejected("""
                {"customerId":"%s","items":[{"name":"Bolo","unitPrice":10}]}
                """.formatted(CUSTOMER_ID), "items[0].quantity", "A quantidade é obrigatória");
    }

    @Test
    void rejectsItemWithZeroQuantity() throws Exception {
        assertRejected("""
                {"customerId":"%s","items":[{"name":"Bolo","quantity":0,"unitPrice":10}]}
                """.formatted(CUSTOMER_ID), "items[0].quantity", "A quantidade deve ser maior que zero");
    }

    @Test
    void rejectsItemWithZeroOrNegativePrice() throws Exception {
        assertRejected("""
                {"customerId":"%s","items":[{"name":"Bolo","quantity":1,"unitPrice":0}]}
                """.formatted(CUSTOMER_ID), "items[0].unitPrice", "O preço unitário deve ser maior que zero");
        assertRejected("""
                {"customerId":"%s","items":[{"name":"Bolo","quantity":1,"unitPrice":-3}]}
                """.formatted(CUSTOMER_ID), "items[0].unitPrice", "O preço unitário deve ser maior que zero");
    }

    @Test
    void rejectsItemPriceWithMoreThanTwoDecimalPlaces() throws Exception {
        assertRejected("""
                {"customerId":"%s","items":[{"name":"Bolo","quantity":1,"unitPrice":10.999}]}
                """.formatted(CUSTOMER_ID), "items[0].unitPrice",
                "O preço unitário deve ter no máximo 10 dígitos inteiros e 2 casas decimais");
    }

    @Test
    void returnsNotFoundWhenCustomerDoesNotExist() throws Exception {
        when(orderService.create(any()))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado: " + CUSTOMER_ID));

        postOrder("""
                {"customerId":"%s","items":[{"name":"Bolo","quantity":1,"unitPrice":10}]}
                """.formatted(CUSTOMER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cliente não encontrado: " + CUSTOMER_ID));
    }

    @Test
    void returnsBadRequestOnBusinessRuleViolation() throws Exception {
        when(orderService.create(any()))
                .thenThrow(new BusinessRuleException("O nome é obrigatório para itens personalizados"));

        postOrder("""
                {"customerId":"%s","items":[{"quantity":1,"unitPrice":10}]}
                """.formatted(CUSTOMER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("O nome é obrigatório para itens personalizados"));
    }

    @Test
    void returnsExistingOrder() throws Exception {
        Order order = sampleOrder();
        when(orderService.findById(order.getId())).thenReturn(order);

        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId().toString()))
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.findById(id)).thenThrow(new ResourceNotFoundException("Pedido não encontrado: " + id));

        mockMvc.perform(get("/api/orders/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pedido não encontrado: " + id));
    }

    @Test
    void listsOrders() throws Exception {
        when(orderService.findAll()).thenReturn(List.of(sampleOrder(), sampleOrder()));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].totalAmount").value(171.00))
                .andExpect(jsonPath("$[0].customerName").value("Maria"));
    }

    @Test
    void returnsStatusAndTotalOnSingleOrder() throws Exception {
        Order order = sampleOrder();
        when(orderService.findById(order.getId())).thenReturn(order);

        mockMvc.perform(get("/api/orders/{id}", order.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(171.00))
                .andExpect(jsonPath("$.description").value("Retirada sábado"));
    }

    private ResultActions patchStatus(Object id, String json) throws Exception {
        return mockMvc.perform(patch("/api/orders/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void changesOrderStatus() throws Exception {
        Order order = sampleOrder();
        order.changeStatus(OrderStatus.IN_PRODUCTION);
        when(orderService.changeStatus(order.getId(), OrderStatus.IN_PRODUCTION)).thenReturn(order);

        patchStatus(order.getId(), "{\"status\":\"IN_PRODUCTION\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PRODUCTION"))
                .andExpect(jsonPath("$.totalAmount").value(171.00));
    }

    @Test
    void returnsBadRequestOnInvalidTransition() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.changeStatus(id, OrderStatus.COMPLETED))
                .thenThrow(new BusinessRuleException("Transição de status inválida: PENDING -> COMPLETED"));

        patchStatus(id, "{\"status\":\"COMPLETED\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Transição de status inválida: PENDING -> COMPLETED"));
    }

    @Test
    void returnsNotFoundWhenChangingStatusOfUnknownOrder() throws Exception {
        UUID id = UUID.randomUUID();
        when(orderService.changeStatus(id, OrderStatus.CANCELLED))
                .thenThrow(new ResourceNotFoundException("Pedido não encontrado: " + id));

        patchStatus(id, "{\"status\":\"CANCELLED\"}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Pedido não encontrado: " + id));
    }

    @Test
    void rejectsMissingOrUnknownStatus() throws Exception {
        UUID id = UUID.randomUUID();

        patchStatus(id, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.status").value("O status é obrigatório"));
        patchStatus(id, "{\"status\":\"SHIPPED\"}").andExpect(status().isBadRequest());
        verify(orderService, never()).changeStatus(any(), any());
    }
}

package com.doceflux.doceflux_api.customer.controller;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.service.CustomerService;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import com.doceflux.doceflux_api.auth.security.SecurityConfig;
import com.doceflux.doceflux_api.auth.security.TokenService;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import({SecurityConfig.class, TokenService.class})
class CustomerControllerTest {

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID OTHER_STORE = UUID.randomUUID();

    private static RequestPostProcessor auth() {
        return auth(STORE);
    }

    private static RequestPostProcessor auth(UUID storeId) {
        return jwt().jwt(j -> j.claim("storeId", storeId.toString()));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private ResultActions postCustomer(String json) throws Exception {
        return mockMvc.perform(post("/api/customers").with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void createsValidCustomer() throws Exception {
        Customer customer = new Customer(STORE, "Maria", "11999999999", "maria@email.com", "Rua A, 10");
        when(customerService.create(eq(STORE), any())).thenReturn(customer);

        postCustomer("""
                {"name":"Maria","phone":"11999999999","email":"maria@email.com","address":"Rua A, 10"}
                """)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/customers/" + customer.getId()))
                .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.name").value("Maria"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void rejectsCustomerWithoutName() throws Exception {
        postCustomer("""
                {"phone":"11999999999"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("O nome é obrigatório"));

        verify(customerService, never()).create(any(), any());
    }

    @Test
    void rejectsCustomerWithoutPhone() throws Exception {
        postCustomer("""
                {"name":"Maria"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("O telefone é obrigatório"));

        verify(customerService, never()).create(any(), any());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        postCustomer("""
                {"name":"Maria","phone":"11999999999","email":"nao-e-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("O email informado é inválido"));

        verify(customerService, never()).create(any(), any());
    }

    @Test
    void returnsExistingCustomer() throws Exception {
        Customer customer = new Customer(STORE, "Maria", "11999999999", null, null);
        when(customerService.findById(customer.getId(), STORE)).thenReturn(customer);

        mockMvc.perform(get("/api/customers/{id}", customer.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.phone").value("11999999999"));
    }

    @Test
    void returnsNotFoundForUnknownCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.findById(id, STORE))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado: " + id));

        mockMvc.perform(get("/api/customers/{id}", id).with(auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cliente não encontrado: " + id));
    }

    @Test
    void listsCustomers() throws Exception {
        when(customerService.search(STORE, null)).thenReturn(List.of(
                new Customer(STORE, "Maria", "11999999999", null, null),
                new Customer(STORE, "Ana", "11888888888", null, null)));

        mockMvc.perform(get("/api/customers").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Maria"))
                .andExpect(jsonPath("$[1].name").value("Ana"));
    }

    @Test
    void passesSearchParameterToService() throws Exception {
        when(customerService.search(STORE, "Maria")).thenReturn(List.of(
                new Customer(STORE, "Maria", "11999999999", null, null)));

        mockMvc.perform(get("/api/customers").with(auth()).param("search", "Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Maria"));
    }

    private ResultActions putCustomer(UUID id, String json) throws Exception {
        return mockMvc.perform(put("/api/customers/{id}", id).with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void updatesCustomerWithoutOptionalFields() throws Exception {
        Customer customer = new Customer(STORE, "Maria Oliveira", "(12) 99999-9999", null, null);
        when(customerService.update(eq(customer.getId()), eq(STORE), any())).thenReturn(customer);

        putCustomer(customer.getId(), """
                {"name":"Maria Oliveira","phone":"(12) 99999-9999","email":null,"address":null}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.name").value("Maria Oliveira"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void updateReturnsNotFoundForUnknownCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.update(eq(id), eq(STORE), any()))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado: " + id));

        putCustomer(id, """
                {"name":"Maria","phone":"11999999999"}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cliente não encontrado: " + id));
    }

    @Test
    void updateRejectsBlankName() throws Exception {
        putCustomer(UUID.randomUUID(), """
                {"name":"  ","phone":"11999999999"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("O nome é obrigatório"));

        verify(customerService, never()).update(any(), any(), any());
    }

    @Test
    void updateRejectsBlankPhone() throws Exception {
        putCustomer(UUID.randomUUID(), """
                {"name":"Maria","phone":""}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("O telefone é obrigatório"));

        verify(customerService, never()).update(any(), any(), any());
    }

    @Test
    void updateRejectsInvalidEmail() throws Exception {
        putCustomer(UUID.randomUUID(), """
                {"name":"Maria","phone":"11999999999","email":"nao-e-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("O email informado é inválido"));

        verify(customerService, never()).update(any(), any(), any());
    }

    @Test
    void customerOfAnotherStoreReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.findById(id, OTHER_STORE))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado: " + id));

        mockMvc.perform(get("/api/customers/{id}", id).with(auth(OTHER_STORE)))
                .andExpect(status().isNotFound());
        verify(customerService).findById(id, OTHER_STORE);
    }

    @Test
    void deletesCustomer() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/customers/" + id).with(auth()))
                .andExpect(status().isNoContent());

        verify(customerService).delete(id, STORE);
    }

    @Test
    void rejectsDeletingCustomerWithOrders() throws Exception {
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new BusinessRuleException("Não é possível excluir um cliente que já possui pedidos registrados."))
                .when(customerService).delete(id, STORE);

        mockMvc.perform(delete("/api/customers/" + id).with(auth()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Não é possível excluir um cliente que já possui pedidos registrados."));
    }
}

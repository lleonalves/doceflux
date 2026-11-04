package com.doceflux.doceflux_api.customer.controller;

import com.doceflux.doceflux_api.customer.domain.Customer;
import com.doceflux.doceflux_api.customer.service.CustomerService;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    private ResultActions postCustomer(String json) throws Exception {
        return mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void createsValidCustomer() throws Exception {
        Customer customer = new Customer("Maria", "11999999999", "maria@email.com", "Rua A, 10");
        when(customerService.create(any())).thenReturn(customer);

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

        verify(customerService, never()).create(any());
    }

    @Test
    void rejectsCustomerWithoutPhone() throws Exception {
        postCustomer("""
                {"name":"Maria"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("O telefone é obrigatório"));

        verify(customerService, never()).create(any());
    }

    @Test
    void rejectsInvalidEmail() throws Exception {
        postCustomer("""
                {"name":"Maria","phone":"11999999999","email":"nao-e-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("O email informado é inválido"));

        verify(customerService, never()).create(any());
    }

    @Test
    void returnsExistingCustomer() throws Exception {
        Customer customer = new Customer("Maria", "11999999999", null, null);
        when(customerService.findById(customer.getId())).thenReturn(customer);

        mockMvc.perform(get("/api/customers/{id}", customer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customer.getId().toString()))
                .andExpect(jsonPath("$.phone").value("11999999999"));
    }

    @Test
    void returnsNotFoundForUnknownCustomer() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.findById(id))
                .thenThrow(new ResourceNotFoundException("Cliente não encontrado: " + id));

        mockMvc.perform(get("/api/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Cliente não encontrado: " + id));
    }

    @Test
    void listsCustomers() throws Exception {
        when(customerService.search(null)).thenReturn(List.of(
                new Customer("Maria", "11999999999", null, null),
                new Customer("Ana", "11888888888", null, null)));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Maria"))
                .andExpect(jsonPath("$[1].name").value("Ana"));
    }

    @Test
    void passesSearchParameterToService() throws Exception {
        when(customerService.search("Maria")).thenReturn(List.of(
                new Customer("Maria", "11999999999", null, null)));

        mockMvc.perform(get("/api/customers").param("search", "Maria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Maria"));
    }

    private ResultActions putCustomer(UUID id, String json) throws Exception {
        return mockMvc.perform(put("/api/customers/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void updatesCustomerWithoutOptionalFields() throws Exception {
        Customer customer = new Customer("Maria Oliveira", "(12) 99999-9999", null, null);
        when(customerService.update(eq(customer.getId()), any())).thenReturn(customer);

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
        when(customerService.update(eq(id), any()))
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

        verify(customerService, never()).update(any(), any());
    }

    @Test
    void updateRejectsBlankPhone() throws Exception {
        putCustomer(UUID.randomUUID(), """
                {"name":"Maria","phone":""}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("O telefone é obrigatório"));

        verify(customerService, never()).update(any(), any());
    }

    @Test
    void updateRejectsInvalidEmail() throws Exception {
        putCustomer(UUID.randomUUID(), """
                {"name":"Maria","phone":"11999999999","email":"nao-e-email"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("O email informado é inválido"));

        verify(customerService, never()).update(any(), any());
    }
}

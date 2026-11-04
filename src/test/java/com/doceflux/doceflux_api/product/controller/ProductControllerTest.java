package com.doceflux.doceflux_api.product.controller;

import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.service.ProductService;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private ResultActions postProduct(String json) throws Exception {
        return mockMvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void assertRejected(String json, String field, String message) throws Exception {
        postProduct(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).value(message));
        verify(productService, never()).create(any());
    }

    @Test
    void createsValidProduct() throws Exception {
        Product product = new Product("Bolo", "Chocolate", new BigDecimal("45.50"));
        when(productService.create(any())).thenReturn(product);

        postProduct("""
                {"name":"Bolo","description":"Chocolate","price":45.50}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value("Bolo"))
                .andExpect(jsonPath("$.price").value(45.50))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void rejectsMissingName() throws Exception {
        assertRejected("""
                {"price":10.00}
                """, "name", "O nome é obrigatório");
    }

    @Test
    void rejectsBlankName() throws Exception {
        assertRejected("""
                {"name":"   ","price":10.00}
                """, "name", "O nome é obrigatório");
    }

    @Test
    void rejectsNameLongerThan120Characters() throws Exception {
        assertRejected("{\"name\":\"" + "a".repeat(121) + "\",\"price\":10.00}",
                "name", "O nome deve ter no máximo 120 caracteres");
    }

    @Test
    void rejectsMissingPrice() throws Exception {
        assertRejected("""
                {"name":"Bolo"}
                """, "price", "O preço é obrigatório");
    }

    @Test
    void rejectsZeroPrice() throws Exception {
        assertRejected("""
                {"name":"Bolo","price":0}
                """, "price", "O preço deve ser maior que zero");
    }

    @Test
    void rejectsNegativePrice() throws Exception {
        assertRejected("""
                {"name":"Bolo","price":-5.00}
                """, "price", "O preço deve ser maior que zero");
    }

    @Test
    void rejectsPriceWithMoreThanTwoDecimalPlaces() throws Exception {
        assertRejected("""
                {"name":"Bolo","price":10.999}
                """, "price", "O preço deve ter no máximo 10 dígitos inteiros e 2 casas decimais");
    }

    @Test
    void rejectsDescriptionLongerThan2000Characters() throws Exception {
        assertRejected("{\"name\":\"Bolo\",\"description\":\"" + "a".repeat(2001) + "\",\"price\":10.00}",
                "description", "A descrição deve ter no máximo 2000 caracteres");
    }

    @Test
    void listsProducts() throws Exception {
        when(productService.findAll(null)).thenReturn(List.of(
                new Product("Brigadeiro", null, new BigDecimal("3.50")),
                new Product("Bolo", "Chocolate", new BigDecimal("40.00"))));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Brigadeiro"))
                .andExpect(jsonPath("$[0].price").value(3.50))
                .andExpect(jsonPath("$[1].description").value("Chocolate"))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    @Test
    void listsOnlyActiveProducts() throws Exception {
        when(productService.findAll(true)).thenReturn(List.of(new Product("Bolo", null, new BigDecimal("40.00"))));

        mockMvc.perform(get("/api/products").param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void listsOnlyInactiveProducts() throws Exception {
        Product inactive = new Product("Torta", null, new BigDecimal("30.00"));
        inactive.changeActive(false);
        when(productService.findAll(false)).thenReturn(List.of(inactive));

        mockMvc.perform(get("/api/products").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void rejectsInvalidActiveFilter() throws Exception {
        mockMvc.perform(get("/api/products").param("active", "talvez"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsExistingProduct() throws Exception {
        Product product = new Product("Bolo", null, new BigDecimal("40.00"));
        when(productService.findById(product.getId())).thenReturn(product);

        mockMvc.perform(get("/api/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value("Bolo"));
    }

    @Test
    void returnsNotFoundForUnknownProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id)).thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        mockMvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produto não encontrado: " + id));
    }

    private ResultActions putProduct(UUID id, String json) throws Exception {
        return mockMvc.perform(put("/api/products/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void updatesValidProduct() throws Exception {
        Product product = new Product("Bolo Premium", "Cobertura", new BigDecimal("55.00"));
        when(productService.update(eq(product.getId()), any())).thenReturn(product);

        putProduct(product.getId(), """
                {"name":"Bolo Premium","description":"Cobertura","price":55.00}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value("Bolo Premium"))
                .andExpect(jsonPath("$.price").value(55.00))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void updateRejectsInvalidPayloads() throws Exception {
        putProduct(UUID.randomUUID(), """
                {"name":"  ","price":10.00}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("O nome é obrigatório"));
        putProduct(UUID.randomUUID(), """
                {"name":"Bolo","price":0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("O preço deve ser maior que zero"));
        putProduct(UUID.randomUUID(), """
                {"name":"Bolo","price":-1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("O preço deve ser maior que zero"));
        putProduct(UUID.randomUUID(), """
                {"name":"Bolo"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("O preço é obrigatório"));

        verify(productService, never()).update(any(), any());
    }

    @Test
    void updateReturnsNotFoundForUnknownProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.update(eq(id), any()))
                .thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        putProduct(id, """
                {"name":"Bolo","price":10.00}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produto não encontrado: " + id));
    }

    private ResultActions patchActive(UUID id, String json) throws Exception {
        return mockMvc.perform(patch("/api/products/{id}/active", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void changesActiveStatus() throws Exception {
        Product product = new Product("Bolo", null, new BigDecimal("40.00"));
        product.changeActive(false);
        when(productService.changeActive(product.getId(), false)).thenReturn(product);

        patchActive(product.getId(), """
                {"active":false}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void rejectsMissingActiveField() throws Exception {
        patchActive(UUID.randomUUID(), """
                {"active":null}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.active").value("O campo active é obrigatório"));
        patchActive(UUID.randomUUID(), "{}").andExpect(status().isBadRequest());

        verify(productService, never()).changeActive(any(), anyBoolean());
    }

    @Test
    void changeActiveReturnsNotFoundForUnknownProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.changeActive(id, true))
                .thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        patchActive(id, """
                {"active":true}
                """)
                .andExpect(status().isNotFound());
    }
}

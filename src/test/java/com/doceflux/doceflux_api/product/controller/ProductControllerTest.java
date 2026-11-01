package com.doceflux.doceflux_api.product.controller;

import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        when(productService.findAll()).thenReturn(List.of(
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
}

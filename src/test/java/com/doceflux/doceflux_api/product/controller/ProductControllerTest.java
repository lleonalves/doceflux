package com.doceflux.doceflux_api.product.controller;

import com.doceflux.doceflux_api.auth.security.SecurityConfig;
import com.doceflux.doceflux_api.auth.security.TokenService;
import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.service.ProductService;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import({SecurityConfig.class, TokenService.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static final UUID STORE = UUID.randomUUID();
    private static final UUID OTHER_STORE = UUID.randomUUID();

    private static RequestPostProcessor auth() {
        return auth(STORE);
    }

    private static RequestPostProcessor auth(UUID storeId) {
        return jwt().jwt(j -> j.claim("storeId", storeId.toString()));
    }

    private ResultActions postProduct(String json) throws Exception {
        return mockMvc.perform(post("/api/products")
                .with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private void assertRejected(String json, String field, String message) throws Exception {
        postProduct(json)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).value(message));
        verify(productService, never()).create(any(), any());
    }

    @Test
    void createsValidProduct() throws Exception {
        Product product = new Product(UUID.randomUUID(), "Bolo", "Chocolate", new BigDecimal("45.50"), 0, null);
        when(productService.create(eq(STORE), any())).thenReturn(product);

        postProduct("""
                {"name":"Bolo","description":"Chocolate","stockQuantity":5,"price":45.50}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value("Bolo"))
                .andExpect(jsonPath("$.price").value(45.50))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
        verify(productService).create(eq(STORE), any());
    }

    @Test
    void rejectsNegativeStockQuantity() throws Exception {
        assertRejected("""
                {"name":"Bolo","price":10.00,"stockQuantity":-1}
                """, "stockQuantity", "A quantidade em estoque não pode ser negativa");
    }

    @Test
    void rejectsMissingStockQuantity() throws Exception {
        assertRejected("""
                {"name":"Bolo","price":10.00}
                """, "stockQuantity", "A quantidade em estoque é obrigatória");
    }

    @Test
    void rejectsMissingName() throws Exception {
        assertRejected("""
                {"stockQuantity":5,"price":10.00}
                """, "name", "O nome é obrigatório");
    }

    @Test
    void rejectsBlankName() throws Exception {
        assertRejected("""
                {"name":"   ","stockQuantity":5,"price":10.00}
                """, "name", "O nome é obrigatório");
    }

    @Test
    void rejectsNameLongerThan120Characters() throws Exception {
        assertRejected("{\"name\":\"" + "a".repeat(121) + "\",\"stockQuantity\":5,\"price\":10.00}",
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
                {"name":"Bolo","stockQuantity":5,"price":0}
                """, "price", "O preço deve ser maior que zero");
    }

    @Test
    void rejectsNegativePrice() throws Exception {
        assertRejected("""
                {"name":"Bolo","stockQuantity":5,"price":-5.00}
                """, "price", "O preço deve ser maior que zero");
    }

    @Test
    void rejectsPriceWithMoreThanTwoDecimalPlaces() throws Exception {
        assertRejected("""
                {"name":"Bolo","stockQuantity":5,"price":10.999}
                """, "price", "O preço deve ter no máximo 10 dígitos inteiros e 2 casas decimais");
    }

    @Test
    void rejectsDescriptionLongerThan2000Characters() throws Exception {
        assertRejected("{\"name\":\"Bolo\",\"description\":\"" + "a".repeat(2001) + "\",\"stockQuantity\":5,\"price\":10.00}",
                "description", "A descrição deve ter no máximo 2000 caracteres");
    }

    @Test
    void listsProducts() throws Exception {
        when(productService.findAll(STORE, null)).thenReturn(List.of(
                new Product(UUID.randomUUID(), "Brigadeiro", null, new BigDecimal("3.50"), 0, null),
                new Product(UUID.randomUUID(), "Bolo", "Chocolate", new BigDecimal("40.00"), 0, null)));

        mockMvc.perform(get("/api/products").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Brigadeiro"))
                .andExpect(jsonPath("$[0].price").value(3.50))
                .andExpect(jsonPath("$[1].description").value("Chocolate"))
                .andExpect(jsonPath("$[1].active").value(true));
    }

    @Test
    void listsOnlyActiveProducts() throws Exception {
        when(productService.findAll(STORE, true)).thenReturn(List.of(new Product(UUID.randomUUID(), "Bolo", null, new BigDecimal("40.00"), 0, null)));

        mockMvc.perform(get("/api/products").with(auth()).param("active", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void listsOnlyInactiveProducts() throws Exception {
        Product inactive = new Product(UUID.randomUUID(), "Torta", null, new BigDecimal("30.00"), 0, null);
        inactive.changeActive(false);
        when(productService.findAll(STORE, false)).thenReturn(List.of(inactive));

        mockMvc.perform(get("/api/products").with(auth()).param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void rejectsInvalidActiveFilter() throws Exception {
        mockMvc.perform(get("/api/products").with(auth()).param("active", "talvez"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsExistingProduct() throws Exception {
        Product product = new Product(UUID.randomUUID(), "Bolo", null, new BigDecimal("40.00"), 0, null);
        when(productService.findById(product.getId(), STORE)).thenReturn(product);

        mockMvc.perform(get("/api/products/{id}", product.getId()).with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(product.getId().toString()))
                .andExpect(jsonPath("$.name").value("Bolo"));
    }

    @Test
    void returnsNotFoundForUnknownProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id, STORE)).thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        mockMvc.perform(get("/api/products/{id}", id).with(auth()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produto não encontrado: " + id));
    }

    private ResultActions putProduct(UUID id, String json) throws Exception {
        return mockMvc.perform(put("/api/products/{id}", id).with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void updatesValidProduct() throws Exception {
        Product product = new Product(UUID.randomUUID(), "Bolo Premium", "Cobertura", new BigDecimal("55.00"), 0, null);
        when(productService.update(eq(product.getId()), eq(STORE), any())).thenReturn(product);

        putProduct(product.getId(), """
                {"name":"Bolo Premium","description":"Cobertura","stockQuantity":5,"price":55.00}
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
                {"name":"  ","stockQuantity":5,"price":10.00}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").value("O nome é obrigatório"));
        putProduct(UUID.randomUUID(), """
                {"name":"Bolo","stockQuantity":5,"price":0}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("O preço deve ser maior que zero"));
        putProduct(UUID.randomUUID(), """
                {"name":"Bolo","stockQuantity":5,"price":-1}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("O preço deve ser maior que zero"));
        putProduct(UUID.randomUUID(), """
                {"name":"Bolo"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").value("O preço é obrigatório"));

        verify(productService, never()).update(any(), any(), any());
    }

    @Test
    void updateReturnsNotFoundForUnknownProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.update(eq(id), eq(STORE), any()))
                .thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        putProduct(id, """
                {"name":"Bolo","stockQuantity":5,"price":10.00}
                """)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Produto não encontrado: " + id));
    }

    private ResultActions patchActive(UUID id, String json) throws Exception {
        return mockMvc.perform(patch("/api/products/{id}/active", id).with(auth())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void changesActiveStatus() throws Exception {
        Product product = new Product(UUID.randomUUID(), "Bolo", null, new BigDecimal("40.00"), 0, null);
        product.changeActive(false);
        when(productService.changeActive(product.getId(), STORE, false)).thenReturn(product);

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

        verify(productService, never()).changeActive(any(), any(), anyBoolean());
    }

    @Test
    void changeActiveReturnsNotFoundForUnknownProduct() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.changeActive(id, STORE, true))
                .thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        patchActive(id, """
                {"active":true}
                """)
                .andExpect(status().isNotFound());
    }

    @Test
    void findByIdOfAnotherStoreReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.findById(id, OTHER_STORE)).thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        mockMvc.perform(get("/api/products/{id}", id).with(auth(OTHER_STORE)))
                .andExpect(status().isNotFound());
        verify(productService).findById(id, OTHER_STORE);
    }

    @Test
    void updateOfAnotherStoreReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.update(eq(id), eq(OTHER_STORE), any()))
                .thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        mockMvc.perform(put("/api/products/{id}", id).with(auth(OTHER_STORE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bolo\",\"stockQuantity\":5,\"price\":10.00}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void changeActiveOfAnotherStoreReturnsNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(productService.changeActive(id, OTHER_STORE, false))
                .thenThrow(new ResourceNotFoundException("Produto não encontrado: " + id));

        mockMvc.perform(patch("/api/products/{id}/active", id).with(auth(OTHER_STORE))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":false}"))
                .andExpect(status().isNotFound());
    }
}

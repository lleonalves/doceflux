package com.doceflux.doceflux_api.product.service;

import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.dto.ProductRequest;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createsActiveProductWithGeneratedIdAndTimestamps() {
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Product created = productService.create(
                new ProductRequest("  Bolo de Cenoura ", "Com cobertura", new BigDecimal("45.5")));

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Bolo de Cenoura");
        assertThat(created.getDescription()).isEqualTo("Com cobertura");
        assertThat(created.getPrice()).isEqualByComparingTo("45.50");
        assertThat(created.getPrice().scale()).isEqualTo(2);
        assertThat(created.isActive()).isTrue();
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getUpdatedAt()).isEqualTo(created.getCreatedAt());
    }

    @Test
    void listsAllProducts() {
        List<Product> products = List.of(
                new Product("Brigadeiro", null, new BigDecimal("3.00")),
                new Product("Bolo", null, new BigDecimal("40.00")));
        when(productRepository.findAll()).thenReturn(products);

        assertThat(productService.findAll()).containsExactlyElementsOf(products);
    }
}

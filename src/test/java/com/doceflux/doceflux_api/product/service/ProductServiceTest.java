package com.doceflux.doceflux_api.product.service;

import com.doceflux.doceflux_api.product.domain.Product;
import com.doceflux.doceflux_api.product.dto.ProductRequest;
import com.doceflux.doceflux_api.product.repository.ProductRepository;
import com.doceflux.doceflux_api.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

        assertThat(productService.findAll(null)).containsExactlyElementsOf(products);
        verify(productRepository, never()).findByActive(anyBoolean());
    }

    @Test
    void listsOnlyActiveOrOnlyInactiveProducts() {
        Product active = new Product("Bolo", null, new BigDecimal("40.00"));
        Product inactive = new Product("Torta", null, new BigDecimal("30.00"));
        inactive.changeActive(false);
        when(productRepository.findByActive(true)).thenReturn(List.of(active));
        when(productRepository.findByActive(false)).thenReturn(List.of(inactive));

        assertThat(productService.findAll(true)).containsExactly(active);
        assertThat(productService.findAll(false)).containsExactly(inactive);
        verify(productRepository, never()).findAll();
    }

    @Test
    void findsExistingProductById() {
        Product product = new Product("Bolo", null, new BigDecimal("40.00"));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThat(productService.findById(product.getId())).isSameAs(product);
    }

    @Test
    void throwsWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesRegistrationDataPreservingActiveStatus() throws InterruptedException {
        Product product = new Product("Bolo", null, new BigDecimal("40.00"));
        product.changeActive(false);
        Instant createdAt = product.getCreatedAt();
        Instant updatedBefore = product.getUpdatedAt();
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        Thread.sleep(2);

        Product updated = productService.update(product.getId(),
                new ProductRequest("  Bolo Premium ", "Com cobertura", new BigDecimal("55.5")));

        assertThat(updated.getName()).isEqualTo("Bolo Premium");
        assertThat(updated.getDescription()).isEqualTo("Com cobertura");
        assertThat(updated.getPrice()).isEqualByComparingTo("55.50");
        assertThat(updated.getPrice().scale()).isEqualTo(2);
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(updatedBefore);
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(id, new ProductRequest("Bolo", null, BigDecimal.TEN)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivatesAndReactivatesProduct() {
        Product product = new Product("Bolo", null, new BigDecimal("40.00"));
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));

        assertThat(productService.changeActive(product.getId(), false).isActive()).isFalse();
        assertThat(productService.changeActive(product.getId(), true).isActive()).isTrue();
    }

    @Test
    void changeActiveThrowsWhenProductDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.changeActive(id, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

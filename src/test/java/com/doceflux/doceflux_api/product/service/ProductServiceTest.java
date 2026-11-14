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

        Product created = productService.create(UUID.randomUUID(),
                new ProductRequest("  Bolo de Cenoura ", "Com cobertura", new BigDecimal("45.5"), 5, null));

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
        UUID storeId = UUID.randomUUID();
        List<Product> products = List.of(
                new Product(storeId, "Brigadeiro", null, new BigDecimal("3.00"), 0, null),
                new Product(storeId, "Bolo", null, new BigDecimal("40.00"), 0, null));
        when(productRepository.findByStoreId(storeId)).thenReturn(products);

        assertThat(productService.findAll(storeId, null)).containsExactlyElementsOf(products);
        verify(productRepository, never()).findByStoreIdAndActive(any(), anyBoolean());
    }

    @Test
    void listsOnlyActiveOrOnlyInactiveProducts() {
        Product active = new Product(UUID.randomUUID(), "Bolo", null, new BigDecimal("40.00"), 0, null);
        Product inactive = new Product(UUID.randomUUID(), "Torta", null, new BigDecimal("30.00"), 0, null);
        inactive.changeActive(false);
        UUID storeId = UUID.randomUUID();
        when(productRepository.findByStoreIdAndActive(storeId, true)).thenReturn(List.of(active));
        when(productRepository.findByStoreIdAndActive(storeId, false)).thenReturn(List.of(inactive));

        assertThat(productService.findAll(storeId, true)).containsExactly(active);
        assertThat(productService.findAll(storeId, false)).containsExactly(inactive);
        verify(productRepository, never()).findByStoreId(any());
    }

    @Test
    void findsExistingProductById() {
        UUID storeId = UUID.randomUUID();
        Product product = new Product(storeId, "Bolo", null, new BigDecimal("40.00"), 0, null);
        when(productRepository.findByIdAndStoreId(product.getId(), storeId)).thenReturn(Optional.of(product));

        assertThat(productService.findById(product.getId(), storeId)).isSameAs(product);
    }

    @Test
    void throwsWhenProductDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndStoreId(id, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(id, storeId)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesRegistrationDataPreservingActiveStatus() throws InterruptedException {
        UUID storeId = UUID.randomUUID();
        Product product = new Product(storeId, "Bolo", null, new BigDecimal("40.00"), 0, null);
        product.changeActive(false);
        Instant createdAt = product.getCreatedAt();
        Instant updatedBefore = product.getUpdatedAt();
        when(productRepository.findByIdAndStoreId(product.getId(), storeId)).thenReturn(Optional.of(product));
        Thread.sleep(2);

        Product updated = productService.update(product.getId(), storeId,
                new ProductRequest("  Bolo Premium ", "Com cobertura", new BigDecimal("55.5"), 5, null));

        assertThat(updated.getName()).isEqualTo("Bolo Premium");
        assertThat(updated.getDescription()).isEqualTo("Com cobertura");
        assertThat(updated.getPrice()).isEqualByComparingTo("55.50");
        assertThat(updated.getPrice().scale()).isEqualTo(2);
        assertThat(updated.getStockQuantity()).isEqualTo(5);
        assertThat(updated.getStoreId()).isEqualTo(storeId);
        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getUpdatedAt()).isAfter(updatedBefore);
        verify(productRepository, never()).save(any());
    }

    @Test
    void updateThrowsWhenProductDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndStoreId(id, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.update(id, storeId, new ProductRequest("Bolo", null, BigDecimal.TEN, 5, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deactivatesAndReactivatesProduct() {
        UUID storeId = UUID.randomUUID();
        Product product = new Product(storeId, "Bolo", null, new BigDecimal("40.00"), 0, null);
        when(productRepository.findByIdAndStoreId(product.getId(), storeId)).thenReturn(Optional.of(product));

        assertThat(productService.changeActive(product.getId(), storeId, false).isActive()).isFalse();
        assertThat(productService.changeActive(product.getId(), storeId, true).isActive()).isTrue();
    }

    @Test
    void changeActiveThrowsWhenProductDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(productRepository.findByIdAndStoreId(id, storeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.changeActive(id, storeId, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

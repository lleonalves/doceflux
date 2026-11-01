package com.doceflux.doceflux_api.order.domain;

import com.doceflux.doceflux_api.product.domain.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    private static final int MONEY_SCALE = 2;

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", updatable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_type", nullable = false, length = 20)
    private OrderItemType itemType;

    @Column(name = "product_name_snapshot", nullable = false, length = 120)
    private String productNameSnapshot;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    protected OrderItem() {
    }

    private OrderItem(Product product, OrderItemType itemType, String name, String description,
                      int quantity, BigDecimal unitPrice) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.itemType = itemType;
        this.productNameSnapshot = name;
        this.description = description;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPrice.setScale(MONEY_SCALE);
        this.subtotal = this.unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }

    public static OrderItem fromCatalog(Product product, String description, int quantity) {
        return new OrderItem(product, OrderItemType.CATALOG, product.getName(), description,
                quantity, product.getPrice());
    }

    public static OrderItem custom(String name, String description, int quantity, BigDecimal unitPrice) {
        return new OrderItem(null, OrderItemType.CUSTOM, name, description, quantity, unitPrice);
    }

    void attachTo(Order order) {
        this.order = order;
    }

    public UUID getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public OrderItemType getItemType() {
        return itemType;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}

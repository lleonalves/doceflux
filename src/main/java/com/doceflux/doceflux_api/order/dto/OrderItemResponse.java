package com.doceflux.doceflux_api.order.dto;

import com.doceflux.doceflux_api.order.domain.OrderItem;
import com.doceflux.doceflux_api.order.domain.OrderItemType;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemResponse(
        UUID id,
        OrderItemType itemType,
        UUID productId,
        String name,
        String description,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getItemType(),
                item.getProduct() == null ? null : item.getProduct().getId(),
                item.getProductNameSnapshot(),
                item.getDescription(),
                item.getQuantity(),
                item.getUnitPriceSnapshot(),
                item.getSubtotal());
    }
}

package com.doceflux.doceflux_api.order.repository;

import com.doceflux.doceflux_api.order.domain.OrderStatus;

import java.math.BigDecimal;

public interface OrderStatusTotals {

    OrderStatus getStatus();

    long getCount();

    BigDecimal getTotalAmount();
}

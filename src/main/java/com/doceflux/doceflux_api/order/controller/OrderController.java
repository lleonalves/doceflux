package com.doceflux.doceflux_api.order.controller;

import com.doceflux.doceflux_api.order.dto.OrderRequest;
import com.doceflux.doceflux_api.order.dto.OrderResponse;
import com.doceflux.doceflux_api.order.service.OrderService;
import com.doceflux.doceflux_api.order.dto.OrderStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Tag(name = "Pedidos")
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        OrderResponse response = OrderResponse.from(orderService.create(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(response.id()).toUri();
        return ResponseEntity.status(HttpStatus.CREATED).location(location).body(response);
    }

    @GetMapping
    public List<OrderResponse> findAll() {
        return orderService.findAll().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse findById(@PathVariable UUID id) {
        return OrderResponse.from(orderService.findById(id));
    }

    @Operation(summary = "Altera o status do pedido",
            description = "Transições permitidas: PENDING -> IN_PRODUCTION | CANCELLED; "
                    + "IN_PRODUCTION -> COMPLETED | CANCELLED. COMPLETED e CANCELLED são estados finais.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status alterado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Transição inválida ou status ausente/inválido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "Pedido não encontrado", content = @Content)
    })
    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody OrderStatusRequest request) {
        return OrderResponse.from(orderService.changeStatus(id, request.status()));
    }
}

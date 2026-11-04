package com.doceflux.doceflux_api.product.controller;

import com.doceflux.doceflux_api.product.dto.ProductActiveRequest;
import com.doceflux.doceflux_api.product.dto.ProductRequest;
import com.doceflux.doceflux_api.product.dto.ProductResponse;
import com.doceflux.doceflux_api.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Produtos")
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @Operation(summary = "Cadastra um produto no catálogo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto cadastrado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = ProductResponse.from(productService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Lista os produtos",
            description = "Sem o filtro active, retorna todos os produtos do catálogo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produtos retornados"),
            @ApiResponse(responseCode = "400", description = "Valor inválido para active", content = @Content)
    })
    @GetMapping
    public List<ProductResponse> findAll(
            @Parameter(description = "Filtra por produtos ativos (true) ou inativos (false)")
            @RequestParam(required = false) Boolean active) {
        return productService.findAll(active).stream().map(ProductResponse::from).toList();
    }

    @Operation(summary = "Consulta um produto pelo identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado"),
            @ApiResponse(responseCode = "400", description = "Identificador inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable UUID id) {
        return ProductResponse.from(productService.findById(id));
    }

    @Operation(summary = "Atualiza os dados cadastrais do produto",
            description = "Altera nome, descrição e preço atuais. Não altera o status ativo e não afeta "
                    + "pedidos já registrados.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(productService.update(id, request));
    }

    @Operation(summary = "Ativa ou inativa o produto",
            description = "Produtos inativos não podem ser incluídos em novos pedidos.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status atualizado"),
            @ApiResponse(responseCode = "400", description = "Campo active ausente ou inválido", content = @Content),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado", content = @Content)
    })
    @PatchMapping("/{id}/active")
    public ProductResponse changeActive(@PathVariable UUID id, @Valid @RequestBody ProductActiveRequest request) {
        return ProductResponse.from(productService.changeActive(id, request.active()));
    }
}

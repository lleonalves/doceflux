package com.doceflux.doceflux_api.auth.controller;

import com.doceflux.doceflux_api.auth.dto.AuthResponse;
import com.doceflux.doceflux_api.auth.dto.LoginRequest;
import com.doceflux.doceflux_api.auth.dto.MeResponse;
import com.doceflux.doceflux_api.auth.dto.RegisterRequest;
import com.doceflux.doceflux_api.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Autenticação")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Cadastro inicial da confeitaria",
            description = "Cria a loja e o usuário gestor (OWNER). Permitido apenas enquanto não houver loja cadastrada.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loja cadastrada e token emitido"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, loja já cadastrada ou email em uso",
                    content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @Operation(summary = "Autentica com email e senha")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado, token emitido"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Dados do usuário autenticado e da loja")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados da sessão atual"),
            @ApiResponse(responseCode = "401", description = "Token ausente, inválido ou expirado",
                    content = @Content)
    })
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(UUID.fromString(jwt.getSubject()));
    }
}

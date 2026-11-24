package com.doceflux.doceflux_api.settings.controller;

import com.doceflux.doceflux_api.settings.dto.ProfileResponse;
import com.doceflux.doceflux_api.settings.dto.UpdatePasswordRequest;
import com.doceflux.doceflux_api.settings.dto.UpdateProfileRequest;
import com.doceflux.doceflux_api.settings.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Configurações")
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @Operation(summary = "Dados da loja e do usuário autenticado")
    @GetMapping("/profile")
    public ProfileResponse getProfile(@AuthenticationPrincipal Jwt jwt) {
        return settingsService.getProfile(userId(jwt), storeId(jwt));
    }

    @Operation(summary = "Atualiza nome da loja e responsáveis")
    @PutMapping("/profile")
    public ProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return settingsService.updateProfile(userId(jwt), storeId(jwt), request);
    }

    @Operation(summary = "Altera a senha do usuário autenticado")
    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody UpdatePasswordRequest request) {
        settingsService.updatePassword(userId(jwt), storeId(jwt), request);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private UUID storeId(Jwt jwt) {
        return UUID.fromString(jwt.getClaimAsString("storeId"));
    }
}

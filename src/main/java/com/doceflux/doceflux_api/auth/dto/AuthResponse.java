package com.doceflux.doceflux_api.auth.dto;

import com.doceflux.doceflux_api.auth.domain.Store;
import com.doceflux.doceflux_api.auth.domain.UserAccount;
import com.doceflux.doceflux_api.auth.domain.UserRole;

import java.time.LocalDate;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UUID userId,
        String email,
        UserRole role,
        UUID storeId,
        String storeName,
        String mainManager,
        String responsibleManager,
        LocalDate companyStartDate) {

    private static final String BEARER = "Bearer";

    public static AuthResponse of(UserAccount user, String accessToken, long expiresIn) {
        Store store = user.getStore();
        return new AuthResponse(accessToken, BEARER, expiresIn, user.getId(), user.getEmail(), user.getRole(),
                store.getId(), store.getStoreName(), store.getMainManager(), store.getResponsibleManager(),
                store.getCompanyStartDate());
    }
}

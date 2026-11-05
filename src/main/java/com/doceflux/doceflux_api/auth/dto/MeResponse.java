package com.doceflux.doceflux_api.auth.dto;

import com.doceflux.doceflux_api.auth.domain.Store;
import com.doceflux.doceflux_api.auth.domain.UserAccount;
import com.doceflux.doceflux_api.auth.domain.UserRole;

import java.time.LocalDate;
import java.util.UUID;

public record MeResponse(
        UUID userId,
        String email,
        UserRole role,
        UUID storeId,
        String storeName,
        String mainManager,
        String responsibleManager,
        LocalDate companyStartDate) {

    public static MeResponse from(UserAccount user) {
        Store store = user.getStore();
        return new MeResponse(user.getId(), user.getEmail(), user.getRole(), store.getId(),
                store.getStoreName(), store.getMainManager(), store.getResponsibleManager(),
                store.getCompanyStartDate());
    }
}

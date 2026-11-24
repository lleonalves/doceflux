package com.doceflux.doceflux_api.settings.dto;

import com.doceflux.doceflux_api.auth.domain.Store;
import com.doceflux.doceflux_api.auth.domain.UserAccount;

import java.time.LocalDate;

public record ProfileResponse(
        String storeName,
        String mainManager,
        String responsibleManager,
        String email,
        LocalDate companyStartDate) {

    public static ProfileResponse from(UserAccount user) {
        Store store = user.getStore();
        return new ProfileResponse(store.getStoreName(), store.getMainManager(),
                store.getResponsibleManager(), user.getEmail(), store.getCompanyStartDate());
    }
}

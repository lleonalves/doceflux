package com.doceflux.doceflux_api.settings.controller;

import com.doceflux.doceflux_api.auth.security.SecurityConfig;
import com.doceflux.doceflux_api.auth.security.TokenService;
import com.doceflux.doceflux_api.settings.dto.ProfileResponse;
import com.doceflux.doceflux_api.settings.service.SettingsService;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettingsController.class)
@Import({SecurityConfig.class, TokenService.class})
class SettingsControllerTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID STORE = UUID.randomUUID();

    private static RequestPostProcessor auth() {
        return jwt().jwt(j -> j.subject(USER.toString()).claim("storeId", STORE.toString()));
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SettingsService settingsService;

    private ProfileResponse profile(String storeName) {
        return new ProfileResponse(storeName, "Maria", "João", "maria@email.com", LocalDate.of(2020, 5, 10));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/settings/profile")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/settings/password")).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsProfileUsingIdsFromJwt() throws Exception {
        when(settingsService.getProfile(USER, STORE)).thenReturn(profile("Doce Sonho"));

        mockMvc.perform(get("/api/settings/profile").with(auth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Doce Sonho"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.mainManager").value("Maria"));
    }

    @Test
    void updatesProfileUsingIdsFromJwt() throws Exception {
        when(settingsService.updateProfile(eq(USER), eq(STORE), any())).thenReturn(profile("Doce Novo"));

        mockMvc.perform(put("/api/settings/profile").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName":"Doce Novo","mainManager":"Maria","responsibleManager":"João"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("Doce Novo"));
    }

    @Test
    void rejectsInvalidProfile() throws Exception {
        mockMvc.perform(put("/api/settings/profile").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"storeName":"  ","mainManager":"Maria","responsibleManager":"João"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.storeName").exists());

        verify(settingsService, never()).updateProfile(any(), any(), any());
    }

    @Test
    void changesPasswordReturningNoContent() throws Exception {
        mockMvc.perform(patch("/api/settings/password").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"senhaAtual1","newPassword":"novaSenha99"}
                                """))
                .andExpect(status().isNoContent());

        verify(settingsService).updatePassword(eq(USER), eq(STORE), any());
    }

    @Test
    void returnsBadRequestForWrongCurrentPassword() throws Exception {
        doThrow(new BusinessRuleException("Senha atual incorreta"))
                .when(settingsService).updatePassword(eq(USER), eq(STORE), any());

        mockMvc.perform(patch("/api/settings/password").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"errada","newPassword":"novaSenha99"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Senha atual incorreta"));
    }

    @Test
    void rejectsShortNewPassword() throws Exception {
        mockMvc.perform(patch("/api/settings/password").with(auth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"senhaAtual1","newPassword":"curta"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").exists());

        verify(settingsService, never()).updatePassword(any(), any(), any());
    }
}

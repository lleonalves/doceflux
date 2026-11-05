package com.doceflux.doceflux_api.auth.controller;

import com.doceflux.doceflux_api.auth.domain.Store;
import com.doceflux.doceflux_api.auth.domain.UserAccount;
import com.doceflux.doceflux_api.auth.domain.UserRole;
import com.doceflux.doceflux_api.auth.dto.AuthResponse;
import com.doceflux.doceflux_api.auth.security.SecurityConfig;
import com.doceflux.doceflux_api.auth.security.TokenService;
import com.doceflux.doceflux_api.auth.service.AuthService;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, TokenService.class})
class AuthControllerTest {

    private static final String VALID_REGISTER = """
            {"storeName":"Doce Sonho","mainManager":"Maria","responsibleManager":"João",
             "companyStartDate":"2020-05-10","email":"maria@email.com","password":"segredo123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    private ResultActions postJson(String url, String json) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private AuthResponse sampleResponse() {
        Store store = new Store("Doce Sonho", "Maria", "João", LocalDate.of(2020, 5, 10));
        UserAccount owner = new UserAccount(store, "maria@email.com", "hash", UserRole.OWNER);
        return AuthResponse.of(owner, "jwt-token", 3600L);
    }

    @Test
    void registersStoreAndReturnsToken() throws Exception {
        when(authService.register(any())).thenReturn(sampleResponse());

        postJson("/api/auth/register", VALID_REGISTER)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.storeName").value("Doce Sonho"))
                .andExpect(jsonPath("$.mainManager").value("Maria"))
                .andExpect(jsonPath("$.responsibleManager").value("João"))
                .andExpect(jsonPath("$.companyStartDate").value("2020-05-10"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsRegistrationWithInvalidFields() throws Exception {
        postJson("/api/auth/register", """
                {"storeName":"","mainManager":"Maria","responsibleManager":"João",
                 "companyStartDate":"2020-05-10","email":"nao-e-email","password":"123"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.storeName").value("O nome da loja é obrigatório"))
                .andExpect(jsonPath("$.errors.email").value("O email informado é inválido"))
                .andExpect(jsonPath("$.errors.password").value("A senha deve ter entre 6 e 100 caracteres"));

        verify(authService, never()).register(any());
    }

    @Test
    void rejectsRegistrationWithoutCompanyStartDate() throws Exception {
        postJson("/api/auth/register", """
                {"storeName":"Doce","mainManager":"Maria","responsibleManager":"João",
                 "email":"maria@email.com","password":"segredo123"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.companyStartDate").value("A data de início da empresa é obrigatória"));
    }

    @Test
    void returnsBadRequestWhenBusinessRuleFails() throws Exception {
        when(authService.register(any())).thenThrow(new BusinessRuleException("Já existe uma loja cadastrada"));

        postJson("/api/auth/register", VALID_REGISTER)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Já existe uma loja cadastrada"));
    }

    @Test
    void logsInWithValidCredentials() throws Exception {
        when(authService.login(any())).thenReturn(sampleResponse());

        postJson("/api/auth/login", """
                {"email":"maria@email.com","password":"segredo123"}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    void returnsUnauthorizedForInvalidCredentials() throws Exception {
        when(authService.login(any())).thenThrow(new InvalidCredentialsException("Email ou senha inválidos"));

        postJson("/api/auth/login", """
                {"email":"maria@email.com","password":"errada"}
                """)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Email ou senha inválidos"));
    }

    @Test
    void rejectsLoginWithMissingFields() throws Exception {
        postJson("/api/auth/login", "{}")
                .andExpect(status().isBadRequest());

        verify(authService, never()).login(any());
    }
}

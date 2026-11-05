package com.doceflux.doceflux_api.auth.controller;

import com.doceflux.doceflux_api.auth.domain.Store;
import com.doceflux.doceflux_api.auth.domain.UserAccount;
import com.doceflux.doceflux_api.auth.domain.UserRole;
import com.doceflux.doceflux_api.auth.dto.MeResponse;
import com.doceflux.doceflux_api.auth.security.SecurityConfig;
import com.doceflux.doceflux_api.auth.security.TokenService;
import com.doceflux.doceflux_api.auth.service.AuthService;
import com.doceflux.doceflux_api.dashboard.controller.DashboardController;
import com.doceflux.doceflux_api.dashboard.service.DashboardService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AuthController.class, DashboardController.class})
@Import({SecurityConfig.class, TokenService.class})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TokenService tokenService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private DashboardService dashboardService;

    private UserAccount owner() {
        Store store = new Store("Doce Sonho", "Maria", "João", LocalDate.of(2020, 5, 10));
        return new UserAccount(store, "maria@email.com", "hash", UserRole.OWNER);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void blocksProtectedRoutesWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/dashboard/summary")
                        .param("startDate", "2026-10-01").param("endDate", "2026-10-31"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blocksMalformedToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer("nao-e-um-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void blocksTokenSignedWithAnotherSecret() throws Exception {
        NimbusJwtEncoder foreignEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(
                new SecretKeySpec("outra-chave-secreta-com-mais-de-32-bytes!".getBytes(StandardCharsets.UTF_8),
                        "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(owner().getId().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        String forged = foreignEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsProtectedRouteWithValidToken() throws Exception {
        UserAccount user = owner();
        when(authService.me(user.getId())).thenReturn(MeResponse.from(user));

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(tokenService.issue(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.storeId").value(user.getStore().getId().toString()))
                .andExpect(jsonPath("$.storeName").value("Doce Sonho"))
                .andExpect(jsonPath("$.mainManager").value("Maria"))
                .andExpect(jsonPath("$.responsibleManager").value("João"))
                .andExpect(jsonPath("$.companyStartDate").value("2020-05-10"));

        verify(authService).me(user.getId());
    }

    @Test
    void keepsRegisterAndLoginPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}

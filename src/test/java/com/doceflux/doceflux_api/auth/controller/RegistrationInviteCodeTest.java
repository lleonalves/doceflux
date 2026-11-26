package com.doceflux.doceflux_api.auth.controller;

import com.doceflux.doceflux_api.auth.repository.StoreRepository;
import com.doceflux.doceflux_api.auth.repository.UserAccountRepository;
import com.doceflux.doceflux_api.auth.security.SecurityConfig;
import com.doceflux.doceflux_api.auth.security.TokenService;
import com.doceflux.doceflux_api.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Usa o AuthService real, com os repositórios simulados. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, TokenService.class, AuthService.class})
@TestPropertySource(properties = "app.security.registration-secret=convite-de-teste")
class RegistrationInviteCodeTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreRepository storeRepository;

    @MockitoBean
    private UserAccountRepository userRepository;

    private ResultActions register(String inviteCodeField) throws Exception {
        return mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"storeName":"Doce Sonho","mainManager":"Maria","responsibleManager":"João",
                 "companyStartDate":"2020-05-10","email":"maria@email.com","password":"segredo123"%s}
                """.formatted(inviteCodeField)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            ",\"inviteCode\":null",
            ",\"inviteCode\":\"\"",
            ",\"inviteCode\":\"errado\"",
            ",\"inviteCode\":\"CONVITE-DE-TESTE\"",
            ",\"inviteCode\":\"convite-de-teste \""
    })
    void blocksRegistrationWithMissingOrWrongInviteCode(String inviteCodeField) throws Exception {
        register(inviteCodeField)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Código de convite inválido ou não informado"));

        verifyNoInteractions(storeRepository, userRepository);
    }

    @Test
    void allowsRegistrationWithCorrectInviteCode() throws Exception {
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        register(",\"inviteCode\":\"convite-de-teste\"")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        verify(userRepository).save(any());
    }
}

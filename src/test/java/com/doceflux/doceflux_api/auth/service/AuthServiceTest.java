package com.doceflux.doceflux_api.auth.service;

import com.doceflux.doceflux_api.auth.domain.Store;
import com.doceflux.doceflux_api.auth.domain.UserAccount;
import com.doceflux.doceflux_api.auth.domain.UserRole;
import com.doceflux.doceflux_api.auth.dto.AuthResponse;
import com.doceflux.doceflux_api.auth.dto.LoginRequest;
import com.doceflux.doceflux_api.auth.dto.MeResponse;
import com.doceflux.doceflux_api.auth.dto.RegisterRequest;
import com.doceflux.doceflux_api.auth.repository.StoreRepository;
import com.doceflux.doceflux_api.auth.repository.UserAccountRepository;
import com.doceflux.doceflux_api.auth.security.TokenService;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.InvalidCredentialsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final LocalDate START_DATE = LocalDate.of(2020, 5, 10);

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest(LocalDate startDate) {
        return new RegisterRequest(" Doce Sonho ", " Maria ", " João ", startDate, "  Maria@Email.COM ", "segredo123");
    }

    private void mockSaves() {
        when(storeRepository.save(any(Store.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tokenService.issue(any())).thenReturn("jwt-token");
        when(tokenService.expiresIn()).thenReturn(3600L);
    }

    private UserAccount existingUser(String rawPassword) {
        Store store = new Store("Doce Sonho", "Maria", "João", START_DATE);
        return new UserAccount(store, "maria@email.com", passwordEncoder.encode(rawPassword), UserRole.OWNER);
    }

    @Test
    void registersStoreAndOwnerWithEncodedPassword() {
        when(storeRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(false);
        mockSaves();

        AuthResponse response = authService.register(registerRequest(START_DATE));

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepository).save(userCaptor.capture());
        UserAccount owner = userCaptor.getValue();
        assertThat(owner.getEmail()).isEqualTo("maria@email.com");
        assertThat(owner.getRole()).isEqualTo(UserRole.OWNER);
        assertThat(owner.isActive()).isTrue();
        assertThat(owner.getPasswordHash()).isNotEqualTo("segredo123").startsWith("$2");
        assertThat(passwordEncoder.matches("segredo123", owner.getPasswordHash())).isTrue();
        assertThat(owner.getStore().getStoreName()).isEqualTo("Doce Sonho");
        assertThat(owner.getStore().getMainManager()).isEqualTo("Maria");
        assertThat(owner.getStore().getResponsibleManager()).isEqualTo("João");
        assertThat(owner.getStore().getCompanyStartDate()).isEqualTo(START_DATE);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(3600L);
        assertThat(response.userId()).isEqualTo(owner.getId());
        assertThat(response.storeId()).isEqualTo(owner.getStore().getId());
        assertThat(response.role()).isEqualTo(UserRole.OWNER);
        assertThat(response.email()).isEqualTo("maria@email.com");
    }

    @Test
    void acceptsCompanyStartDateOfToday() {
        when(storeRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(false);
        mockSaves();

        assertThat(authService.register(registerRequest(LocalDate.now()))).isNotNull();
    }

    @Test
    void rejectsSecondStore() {
        when(storeRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(registerRequest(START_DATE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(storeRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicateEmail() {
        when(storeRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest(START_DATE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(storeRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void rejectsFutureCompanyStartDate() {
        when(storeRepository.count()).thenReturn(0L);
        when(userRepository.existsByEmail("maria@email.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.register(registerRequest(LocalDate.now().plusDays(1))))
                .isInstanceOf(BusinessRuleException.class);

        verify(storeRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void logsInWithValidCredentialsIgnoringEmailCaseAndSpaces() {
        UserAccount user = existingUser("segredo123");
        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));
        when(tokenService.issue(user)).thenReturn("jwt-token");
        when(tokenService.expiresIn()).thenReturn(3600L);

        AuthResponse response = authService.login(new LoginRequest(" Maria@Email.com ", "segredo123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo(user.getId());
        assertThat(response.storeName()).isEqualTo("Doce Sonho");
    }

    @Test
    void rejectsLoginWithWrongPassword() {
        UserAccount user = existingUser("segredo123");
        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@email.com", "errada")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsLoginWithUnknownEmail() {
        when(userRepository.findByEmail("ninguem@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@email.com", "segredo123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsLoginOfInactiveUser() {
        UserAccount user = existingUser("segredo123");
        ReflectionTestUtils.setField(user, "active", false);
        when(userRepository.findByEmail("maria@email.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("maria@email.com", "segredo123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void returnsCurrentUserAndStoreData() {
        UserAccount user = existingUser("segredo123");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        MeResponse me = authService.me(user.getId());

        assertThat(me.userId()).isEqualTo(user.getId());
        assertThat(me.email()).isEqualTo("maria@email.com");
        assertThat(me.role()).isEqualTo(UserRole.OWNER);
        assertThat(me.storeName()).isEqualTo("Doce Sonho");
        assertThat(me.mainManager()).isEqualTo("Maria");
        assertThat(me.responsibleManager()).isEqualTo("João");
        assertThat(me.companyStartDate()).isEqualTo(START_DATE);
    }

    @Test
    void rejectsSessionOfUnknownUser() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(id)).isInstanceOf(InvalidCredentialsException.class);
    }
}

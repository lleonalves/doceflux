package com.doceflux.doceflux_api.settings.service;

import com.doceflux.doceflux_api.auth.domain.Store;
import com.doceflux.doceflux_api.auth.domain.UserAccount;
import com.doceflux.doceflux_api.auth.domain.UserRole;
import com.doceflux.doceflux_api.auth.repository.UserAccountRepository;
import com.doceflux.doceflux_api.settings.dto.ProfileResponse;
import com.doceflux.doceflux_api.settings.dto.UpdatePasswordRequest;
import com.doceflux.doceflux_api.settings.dto.UpdateProfileRequest;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.InvalidCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingsServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Spy
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks
    private SettingsService settingsService;

    private UserAccount user;
    private UUID userId;
    private UUID storeId;

    @BeforeEach
    void setUp() {
        Store store = new Store("Doce Sonho", "Maria", "João", LocalDate.of(2020, 5, 10));
        user = new UserAccount(store, "maria@email.com", passwordEncoder.encode("senhaAtual1"), UserRole.OWNER);
        userId = user.getId();
        storeId = store.getId();
    }

    @Test
    void returnsProfileOfAuthenticatedUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ProfileResponse profile = settingsService.getProfile(userId, storeId);

        assertThat(profile.storeName()).isEqualTo("Doce Sonho");
        assertThat(profile.mainManager()).isEqualTo("Maria");
        assertThat(profile.responsibleManager()).isEqualTo("João");
        assertThat(profile.email()).isEqualTo("maria@email.com");
        assertThat(profile.companyStartDate()).isEqualTo(LocalDate.of(2020, 5, 10));
    }

    @Test
    void updatesStoreProfileTrimmingValuesAndKeepingEmail() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ProfileResponse profile = settingsService.updateProfile(userId, storeId,
                new UpdateProfileRequest("  Doce Novo ", " Ana ", " Pedro "));

        assertThat(profile.storeName()).isEqualTo("Doce Novo");
        assertThat(profile.mainManager()).isEqualTo("Ana");
        assertThat(profile.responsibleManager()).isEqualTo("Pedro");
        assertThat(profile.email()).isEqualTo("maria@email.com");
        assertThat(user.getStore().getStoreName()).isEqualTo("Doce Novo");
    }

    @Test
    void changesPasswordStoringEncodedHash() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        settingsService.updatePassword(userId, storeId, new UpdatePasswordRequest("senhaAtual1", "novaSenha99"));

        assertThat(passwordEncoder.matches("novaSenha99", user.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("senhaAtual1", user.getPasswordHash())).isFalse();
    }

    @Test
    void rejectsWrongCurrentPasswordWithoutChangingHash() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        String originalHash = user.getPasswordHash();

        assertThatThrownBy(() -> settingsService.updatePassword(userId, storeId,
                new UpdatePasswordRequest("errada123", "novaSenha99")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Senha atual incorreta");

        assertThat(user.getPasswordHash()).isEqualTo(originalHash);
    }

    @Test
    void blocksAccessWhenUserBelongsToAnotherStore() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        UUID otherStore = UUID.randomUUID();

        assertThatThrownBy(() -> settingsService.getProfile(userId, otherStore))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> settingsService.updateProfile(userId, otherStore,
                new UpdateProfileRequest("Invasor", "X", "Y")))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> settingsService.updatePassword(userId, otherStore,
                new UpdatePasswordRequest("senhaAtual1", "novaSenha99")))
                .isInstanceOf(InvalidCredentialsException.class);

        assertThat(user.getStore().getStoreName()).isEqualTo("Doce Sonho");
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> settingsService.getProfile(userId, storeId))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}

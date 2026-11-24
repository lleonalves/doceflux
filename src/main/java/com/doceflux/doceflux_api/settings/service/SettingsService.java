package com.doceflux.doceflux_api.settings.service;

import com.doceflux.doceflux_api.auth.domain.UserAccount;
import com.doceflux.doceflux_api.auth.repository.UserAccountRepository;
import com.doceflux.doceflux_api.settings.dto.ProfileResponse;
import com.doceflux.doceflux_api.settings.dto.UpdatePasswordRequest;
import com.doceflux.doceflux_api.settings.dto.UpdateProfileRequest;
import com.doceflux.doceflux_api.shared.BusinessRuleException;
import com.doceflux.doceflux_api.shared.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SettingsService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SettingsService(UserAccountRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId, UUID storeId) {
        return ProfileResponse.from(findUser(userId, storeId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UUID storeId, UpdateProfileRequest request) {
        UserAccount user = findUser(userId, storeId);
        user.getStore().updateProfile(request.storeName().trim(), request.mainManager().trim(),
                request.responsibleManager().trim());
        return ProfileResponse.from(user);
    }

    @Transactional
    public void updatePassword(UUID userId, UUID storeId, UpdatePasswordRequest request) {
        UserAccount user = findUser(userId, storeId);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessRuleException("Senha atual incorreta");
        }
        user.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private UserAccount findUser(UUID userId, UUID storeId) {
        return userRepository.findById(userId)
                .filter(UserAccount::isActive)
                .filter(user -> user.getStore().getId().equals(storeId))
                .orElseThrow(() -> new InvalidCredentialsException("Sessão inválida"));
    }
}

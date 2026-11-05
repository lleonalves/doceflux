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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final StoreRepository storeRepository;
    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(StoreRepository storeRepository, UserAccountRepository userRepository,
                       PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (storeRepository.count() > 0) {
            throw new BusinessRuleException("Já existe uma loja cadastrada neste sistema");
        }
        if (userRepository.existsByEmail(email)) {
            throw new BusinessRuleException("O email informado já está em uso");
        }
        if (request.companyStartDate().isAfter(LocalDate.now())) {
            throw new BusinessRuleException("A data de início da empresa não pode ser futura");
        }

        Store store = storeRepository.save(new Store(
                request.storeName().trim(),
                request.mainManager().trim(),
                request.responsibleManager().trim(),
                request.companyStartDate()));
        UserAccount owner = userRepository.save(new UserAccount(
                store, email, passwordEncoder.encode(request.password()), UserRole.OWNER));
        return authResponse(owner);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UserAccount user = userRepository.findByEmail(normalizeEmail(request.email()))
                .filter(UserAccount::isActive)
                .filter(found -> passwordEncoder.matches(request.password(), found.getPasswordHash()))
                .orElseThrow(() -> new InvalidCredentialsException("Email ou senha inválidos"));
        return authResponse(user);
    }

    @Transactional(readOnly = true)
    public MeResponse me(UUID userId) {
        UserAccount user = userRepository.findById(userId)
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new InvalidCredentialsException("Sessão inválida"));
        return MeResponse.from(user);
    }

    private AuthResponse authResponse(UserAccount user) {
        return AuthResponse.of(user, tokenService.issue(user), tokenService.expiresIn());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

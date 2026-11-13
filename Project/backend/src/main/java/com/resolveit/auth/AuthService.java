package com.resolveit.auth;

import com.resolveit.common.Constants;
import com.resolveit.config.AppProperties;
import com.resolveit.entity.PasswordResetToken;
import com.resolveit.entity.RefreshToken;
import com.resolveit.entity.Role;
import com.resolveit.entity.User;
import com.resolveit.exception.BadRequestException;
import com.resolveit.exception.ConflictException;
import com.resolveit.exception.ResourceNotFoundException;
import com.resolveit.exception.UnauthorizedException;
import com.resolveit.notification.EmailNotificationEvent;
import com.resolveit.security.JwtService;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.RoleRepository;
import com.resolveit.user.UserDto;
import com.resolveit.user.UserRepository;
import com.resolveit.user.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AppProperties appProperties;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AppProperties appProperties,
                       UserService userService,
                       ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.appProperties = appProperties;
        this.userService = userService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public AuthDtos.AuthResponse register(AuthDtos.RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("EMAIL_EXISTS", "An account with this email already exists");
        }
        Role userRole = roleRepository.findByName(Constants.ROLE_USER)
                .orElseThrow(() -> new IllegalStateException("ROLE_USER not seeded"));

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(userRole);
        user.setActive(true);
        user = userRepository.save(user);

        return issueTokens(user);
    }

    @Transactional(noRollbackFor = {BadCredentialsException.class, LockedException.class})
    public AuthDtos.AuthResponse login(AuthDtos.LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
            throw new LockedException("Account temporarily locked due to repeated failed attempts. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid email or password");
        }

        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        return issueTokens(user);
    }

    @Transactional
    public AuthDtos.AuthResponse refresh(AuthDtos.RefreshRequest request) {
        String hash = JwtService.sha256Hex(request.refreshToken());
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (token.isRevoked()) {
            // Reuse detected: revoke all active tokens for the user as a defensive safeguard.
            refreshTokenRepository.revokeAllActiveByUserId(token.getUser().getId(), Instant.now());
            throw new UnauthorizedException("Refresh token has been revoked");
        }

        if (token.isExpired()) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = token.getUser();
        if (!user.isActive()) {
            throw new UnauthorizedException("Account is inactive");
        }

        token.setRevokedAt(Instant.now());
        refreshTokenRepository.save(token);

        return issueTokens(user);
    }

    @Transactional
    public void logout(Long userId, AuthDtos.LogoutRequest request) {
        if (request != null && request.refreshToken() != null && !request.refreshToken().isBlank()) {
            String hash = JwtService.sha256Hex(request.refreshToken());
            refreshTokenRepository.findByTokenHash(hash).ifPresent(rt -> {
                if (rt.getUser().getId().equals(userId) && rt.getRevokedAt() == null) {
                    rt.setRevokedAt(Instant.now());
                    refreshTokenRepository.save(rt);
                }
            });
        } else {
            refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
        }
    }

    @Transactional
    public void forgotPassword(AuthDtos.ForgotPasswordRequest request) {
        // Generic non-enumerating flow: never reveal whether the email exists.
        userRepository.findByEmailIgnoreCase(request.email().trim())
                .filter(User::isActive)
                .ifPresent(user -> {
                    String rawToken = generateOpaqueToken();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setUser(user);
                    resetToken.setTokenHash(JwtService.sha256Hex(rawToken));
                    resetToken.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
                    passwordResetTokenRepository.save(resetToken);

                    log.info("Generated password reset token for userId={} (email delivery enabled={})",
                            user.getId(), appProperties.mail().enabled());
                    eventPublisher.publishEvent(new EmailNotificationEvent(
                            user.getEmail(),
                            "[ResolveIt] Password Reset Request",
                            "Use this reset token within 30 minutes to reset your password: " + rawToken
                    ));
                });
    }

    @Transactional
    public void resetPassword(AuthDtos.ResetPasswordRequest request) {
        String hash = JwtService.sha256Hex(request.token().trim());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired reset token"));

        if (resetToken.getUsedAt() != null || Instant.now().isAfter(resetToken.getExpiresAt())) {
            throw new BadRequestException("INVALID_RESET_TOKEN", "Invalid or expired reset token");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        resetToken.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(resetToken);
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), Instant.now());
    }

    @Transactional(readOnly = true)
    public UserDto me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userService.toUserDtoWithDepartments(user);
    }

    @Transactional
    public UserDto updateProfile(Long userId, AuthDtos.UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setName(request.name().trim());

        if (request.newPassword() != null && !request.newPassword().isBlank()) {
            if (request.currentPassword() == null ||
                    !passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
                throw new BadRequestException("INVALID_CURRENT_PASSWORD", "Current password is incorrect");
            }
            user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        }

        return userService.toUserDtoWithDepartments(userRepository.save(user));
    }

    private AuthDtos.AuthResponse issueTokens(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefreshToken = generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(JwtService.sha256Hex(rawRefreshToken));
        refreshToken.setExpiresAt(Instant.now().plusSeconds(appProperties.jwt().refreshTtlSeconds()));
        refreshTokenRepository.save(refreshToken);

        return new AuthDtos.AuthResponse(
                accessToken,
                rawRefreshToken,
                "Bearer",
                appProperties.jwt().accessTtlSeconds(),
                userService.toUserDtoWithDepartments(user)
        );
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}


package com.resolveit.auth;

import com.resolveit.common.ApiResponse;
import com.resolveit.security.UserPrincipal;
import com.resolveit.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new public user account")
    public ApiResponse<AuthDtos.AuthResponse> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return ApiResponse.ok("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate credentials and issue access + refresh tokens")
    public ApiResponse<AuthDtos.AuthResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token and issue a new access token")
    public ApiResponse<AuthDtos.AuthResponse> refresh(@Valid @RequestBody AuthDtos.RefreshRequest request) {
        return ApiResponse.ok("Token refreshed", authService.refresh(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke active refresh token(s)")
    public ApiResponse<Void> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) AuthDtos.LogoutRequest request
    ) {
        authService.logout(principal.getId(), request);
        return ApiResponse.okMessage("Logged out successfully");
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset token (non-enumerating)")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody AuthDtos.ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.okMessage("If an account matches that email, reset instructions have been issued");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a valid reset token")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody AuthDtos.ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.okMessage("Password reset successful");
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile and role")
    public ApiResponse<UserDto> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.ok(authService.me(principal.getId()));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current authenticated user's profile")
    public ApiResponse<UserDto> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AuthDtos.UpdateProfileRequest request
    ) {
        return ApiResponse.ok("Profile updated", authService.updateProfile(principal.getId(), request));
    }
}


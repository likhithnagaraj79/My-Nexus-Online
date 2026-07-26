package com.exhibitorreg.auth;

import com.exhibitorreg.auth.dto.ChangePasswordRequest;
import com.exhibitorreg.auth.dto.LoginRequest;
import com.exhibitorreg.auth.dto.LoginResponse;
import com.exhibitorreg.auth.dto.LogoutRequest;
import com.exhibitorreg.auth.dto.RefreshRequest;
import com.exhibitorreg.auth.dto.TokenPair;
import com.exhibitorreg.auth.dto.TotpLoginRequest;
import com.exhibitorreg.common.web.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(
                request, ClientIpResolver.resolve(servletRequest), servletRequest.getHeader("User-Agent"));
    }

    @PostMapping("/login/totp")
    public LoginResponse loginTotp(@Valid @RequestBody TotpLoginRequest request, HttpServletRequest servletRequest) {
        return authService.completeTotpLogin(
                request, ClientIpResolver.resolve(servletRequest), servletRequest.getHeader("User-Agent"));
    }

    @PostMapping("/refresh")
    public TokenPair refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public void logout(
            @Valid @RequestBody LogoutRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal,
            HttpServletRequest servletRequest) {
        authService.logout(
                request.refreshToken(),
                principal,
                ClientIpResolver.resolve(servletRequest),
                servletRequest.getHeader("User-Agent"));
    }

    @PostMapping("/change-password")
    public TokenPair changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return authService.changePassword(principal, request);
    }
}

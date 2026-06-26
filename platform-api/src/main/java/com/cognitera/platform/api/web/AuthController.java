package com.cognitera.platform.api.web;

import com.cognitera.platform.api.dto.auth.AuthResponse;
import com.cognitera.platform.api.dto.auth.CurrentUserResponse;
import com.cognitera.platform.api.dto.auth.LoginRequest;
import com.cognitera.platform.api.dto.auth.LogoutRequest;
import com.cognitera.platform.api.dto.auth.RefreshTokenRequest;
import com.cognitera.platform.api.dto.auth.RegisterRequest;
import com.cognitera.platform.auth.api.AuthFacade;
import com.cognitera.platform.auth.api.LoginCommand;
import com.cognitera.platform.auth.api.LogoutCommand;
import com.cognitera.platform.auth.api.RefreshTokenCommand;
import com.cognitera.platform.auth.api.RegisterUserCommand;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints: register, login, token refresh, logout, and current user.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacade authFacade;

    /**
     * Constructs the controller with the auth facade.
     */
    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    /**
     * Registers a new user account.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthResponse.from(authFacade.register(new RegisterUserCommand(
                request.email(),
                request.password(),
                request.displayName(),
                request.roles())));
    }

    /**
     * Authenticates a user with email and password and returns tokens.
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return AuthResponse.from(authFacade.login(new LoginCommand(
                request.email(),
                request.password(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"))));
    }

    /**
     * Issues a new access token using a valid refresh token.
     */
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshTokenRequest request, HttpServletRequest servletRequest) {
        return AuthResponse.from(authFacade.refresh(new RefreshTokenCommand(
                request.refreshToken(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent"))));
    }

    /**
     * Invalidates the given refresh token, logging the user out.
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request, Authentication authentication) {
        authFacade.logout(new LogoutCommand(request.refreshToken(), authentication.getName()));
    }

    /**
     * Returns the currently authenticated user's details.
     */
    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        return CurrentUserResponse.from(authFacade.currentUser(authentication.getName()));
    }
}

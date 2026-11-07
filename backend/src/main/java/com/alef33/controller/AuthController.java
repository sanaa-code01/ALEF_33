package com.alef33.controller;

import com.alef33.dto.LoginRequest;
import com.alef33.dto.LoginResponse;
import com.alef33.dto.UserResponse;
import com.alef33.model.AppUser;
import com.alef33.security.JwtService;
import com.alef33.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password, get a JWT token")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AppUser user = userService.authenticate(request.email(), request.password());
        JwtService.IssuedToken token = jwtService.issue(user);
        return new LoginResponse(token.value(), "Bearer", token.expiresAt(),
                user.getEmail(), user.getFullName(), user.getRole());
    }

    @GetMapping("/me")
    @Operation(summary = "Who am I? (the user of the current token)")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.getByEmail(jwt.getSubject()));
    }
}

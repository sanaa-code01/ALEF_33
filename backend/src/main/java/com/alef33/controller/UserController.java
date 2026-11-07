package com.alef33.controller;

import com.alef33.dto.UserRequest;
import com.alef33.dto.UserResponse;
import com.alef33.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Account management, ADMIN only (see SecurityConfig) */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users (admin)")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List user accounts")
    public List<UserResponse> list() {
        return userService.list().stream().map(UserResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a user account (ADMIN or EMPLOYEE)")
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return UserResponse.from(userService.create(request));
    }

    @PatchMapping("/{id}/enabled")
    @Operation(summary = "Disable (enabled=false) or re-enable (enabled=true) an account")
    public UserResponse setEnabled(@PathVariable Long id, @RequestParam boolean enabled,
                                   @AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(userService.setEnabled(id, enabled, jwt.getSubject()));
    }
}

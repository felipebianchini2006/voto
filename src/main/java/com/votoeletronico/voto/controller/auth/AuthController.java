package com.votoeletronico.voto.controller.auth;

import com.votoeletronico.voto.dto.request.CandidateRegistrationRequest;
import com.votoeletronico.voto.dto.request.LoginRequest;
import com.votoeletronico.voto.dto.response.AuthResponse;
import com.votoeletronico.voto.dto.response.UserResponse;
import com.votoeletronico.voto.service.AuthenticationService;
import com.votoeletronico.voto.service.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller for login and registration
 */
@Tag(name = "Authentication", description = "Authentication endpoints for login and registration")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserMapper userMapper;

    @Operation(summary = "Login", description = "Authenticate user and get JWT token")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authenticationService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Candidate Registration", description = "Self-registration for candidates")
    @PostMapping("/register/candidate")
    public ResponseEntity<AuthResponse> registerCandidate(
            @Valid @RequestBody CandidateRegistrationRequest request) {
        AuthResponse response = authenticationService.registerCandidate(request);
        return ResponseEntity.status(201).body(response);
    }

    @Operation(summary = "Get Current User", description = "Get authenticated user information")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        var user = authenticationService.getCurrentUser(authentication);
        return ResponseEntity.ok(userMapper.toResponse(user));
    }
}

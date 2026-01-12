package application.authenticationservice.controller;

import application.authenticationservice.dto.*;
import application.authenticationservice.util.*;
import application.authenticationservice.entity.User;
import application.authenticationservice.exceptions.UnauthorizedException;
import application.authenticationservice.service.AuthService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtHandler jwtHandler;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader("X-User-Role") String adminRole
    ) {
        // Only ADMINs can register
        if (!"ADMIN".equals(adminRole)) {
            throw new UnauthorizedException("Only admins can register users");
        }

        // Pass the adminId to the service
        AuthResponse response = authService.register(request, adminId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);

            // If 2FA is required, return 202 Accepted status (no token yet)
            if (response.getRequires2FA() != null && response.getRequires2FA()) {
                return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
            }

            // Return 200 OK with JWT token
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
    }

    // PROTECTED ENDPOINTS - JWT required
    @PostMapping("/2fa/setup/{userId}")
    public ResponseEntity<?> setupTwoFA(@PathVariable Long userId) {
        try {
            TwoFASetupResponse response = authService.setupTwoFA(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = Map.of("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }


    @PostMapping("/2fa/verify/{userId}")
    public ResponseEntity<AuthResponse> verifyAndEnable2FA(
            @PathVariable Long userId,
            @RequestBody TwoFAVerificationRequest request) {
        try {
            AuthResponse response = authService.verifyAndEnable2FA(userId, request.getCode());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setTwoFaEnabled(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/2fa/disable/{userId}")
    public ResponseEntity<AuthResponse> disable2FA(@PathVariable Long userId) {
        try {
            AuthResponse response = authService.disable2FA(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            errorResponse.setTwoFaEnabled(false);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    @PostMapping("/google/avatar")
    public ResponseEntity<AuthResponse> fetchGoogleAvatar(
            @RequestBody GoogleProfileRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        try {
            // Extract userId from your JWT
            String token = authorizationHeader.replace("Bearer ", "");
            Long userId = Long.parseLong(jwtHandler.extractUserId(token));

            AuthResponse response = authService.fetchGoogleProfile(userId, request.getAccessToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }


    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        try {
            User user = authService.getUserById(userId);
            return ResponseEntity.ok(user);
        } catch (RuntimeException e) {
            Map<String, String> error = Map.of("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PostMapping("/login/google")
    public ResponseEntity<AuthResponse> loginWithGoogle(
            @RequestBody GoogleLoginRequest request) {

        try {
            AuthResponse response = authService.loginWithGoogle(request.getAccessToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            AuthResponse errorResponse = new AuthResponse();
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }
    @GetMapping("/validate")
    public ResponseEntity<String> validateService() {
        return ResponseEntity.ok("Authentication service is running");
    }

    @GetMapping("/all-users")
    public ResponseEntity<List<AuthResponse>> getAllUsers() {

        List<User> users = authService.getAllUsers();

        List<AuthResponse> responses = users.stream()
                .map(user -> new AuthResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        user.getRole().name(),
                        user.getProfilePictureUrl(),
                        user.getTwoFaEnabled(),
                        false,
                        "User fetched"
                ))
                .toList();

        return ResponseEntity.ok(responses);
    }


}
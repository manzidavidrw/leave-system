package application.authenticationservice.service;

import application.authenticationservice.Enum.Role;
import application.authenticationservice.dto.AuthResponse;
import application.authenticationservice.dto.LoginRequest;
import application.authenticationservice.dto.RegisterRequest;
import application.authenticationservice.dto.TwoFASetupResponse;
import application.authenticationservice.entity.User;
import application.authenticationservice.exceptions.AdminNotFoundException;
import application.authenticationservice.exceptions.UnauthorizedException;
import application.authenticationservice.repository.UserRepository;
import application.authenticationservice.util.JwtHandler;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GoogleAuthenticator googleAuthenticator;
    private final RestTemplate restTemplate;
    private final JwtHandler jwtHandler;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request, Long adminId) {

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new AdminNotFoundException("Admin not found"));

        if (request.getRole() == null) {
            throw new RuntimeException("Role is required");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new RuntimeException("Cannot register admin users");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole());
        user.setEnabled(true);
        user.setTwoFaEnabled(false);

        User savedUser = userRepository.save(user);

        return new AuthResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedUser.getFirstName(),
                savedUser.getLastName(),
                savedUser.getRole().name(),
                savedUser.getProfilePictureUrl(),
                savedUser.getTwoFaEnabled(),
                false,
                "User registered successfully"
        );
    }


    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.getEnabled()) {
            throw new RuntimeException("Account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // Check if 2FA is enabled
        if (user.getTwoFaEnabled()) {
            if (request.getTwoFaCode() == null || request.getTwoFaCode().isEmpty()) {
                // Return response indicating 2FA is required (no token yet)
                AuthResponse response = new AuthResponse();
                response.setUserId(user.getId());
                response.setTwoFaEnabled(true);
                response.setRequires2FA(true);
                response.setMessage("2FA code required");
                return response;
            }

            // Verify 2FA code
            boolean isValid = googleAuthenticator.authorize(
                    user.getTwoFaSecret(),
                    Integer.parseInt(request.getTwoFaCode())
            );

            if (!isValid) {
                throw new RuntimeException("Invalid 2FA code");
            }
        }

        // Generate JWT token using JwtHandler
        String accessToken = jwtHandler.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getProfilePictureUrl(),
                user.getTwoFaEnabled(),
                false,
                "Login successful"
        );

        response.setAccessToken(accessToken);
        response.setExpiresIn(jwtExpiration);

        return response;
    }

    @Transactional
    public TwoFASetupResponse setupTwoFA(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate secret key
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        // Save secret to user
        user.setTwoFaSecret(secret);
        user.setTwoFaEnabled(false); // Will be enabled after verification
        userRepository.save(user);

        // Generate QR code URL
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                "AuthService",
                user.getEmail(),
                key
        );

        return new TwoFASetupResponse(
                secret,
                qrCodeUrl,
                "Scan QR code with Google Authenticator app"
        );
    }

    @Transactional
    public AuthResponse verifyAndEnable2FA(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTwoFaSecret() == null) {
            throw new RuntimeException("2FA not setup. Call setup endpoint first");
        }

        // Verify 2FA code
        boolean isValid = googleAuthenticator.authorize(
                user.getTwoFaSecret(),
                Integer.parseInt(code)
        );

        if (!isValid) {
            throw new RuntimeException("Invalid 2FA code");
        }

        // Enable 2FA after successful verification
        user.setTwoFaEnabled(true);
        userRepository.save(user);

        // ✅ Generate JWT using your JwtHandler
        String token = jwtHandler.generateToken(
                user.getId().toString(),
                user.getEmail(),
                user.getRole().name()
        );

        // Build AuthResponse including the token
        AuthResponse response = new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getProfilePictureUrl(),
                true,
                false,
                "2FA verified successfully"
        );

        response.setAccessToken(token);
        response.setExpiresIn(jwtExpiration);

        return response;
    }


    @Transactional
    public AuthResponse disable2FA(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTwoFaEnabled(false);
        user.setTwoFaSecret(null);
        userRepository.save(user);

        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.getProfilePictureUrl(),
                false,
                false,
                "2FA disabled successfully"
        );
    }

    @Transactional
    public AuthResponse fetchGoogleProfile(Long userId, String accessToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // Call Google API to get user profile info
            String googleApiUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    googleApiUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> profileData = response.getBody();

            if (profileData != null) {
                String pictureUrl = (String) profileData.get("picture");
                user.setProfilePictureUrl(pictureUrl);
                userRepository.save(user);
            }

            return new AuthResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name(),
                    user.getProfilePictureUrl(),
                    user.getTwoFaEnabled(),
                    false,
                    "Profile picture fetched successfully"
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch Google profile: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse loginWithGoogle(String accessToken) {
        try {
            // 1️⃣ Get user info from Google
            String googleApiUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    googleApiUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map<String, Object> profileData = response.getBody();

            if (profileData == null) {
                throw new RuntimeException("Failed to fetch Google profile");
            }

            String email = (String) profileData.get("email");
            String firstName = (String) profileData.get("given_name");
            String lastName = (String) profileData.get("family_name");
            String picture = (String) profileData.get("picture");
            String googleId = (String) profileData.get("id");

            // 2️⃣ Check if user already exists
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> {
                        // 3️⃣ If not, create a new user automatically
                        User newUser = new User();
                        newUser.setEmail(email);
                        newUser.setFirstName(firstName);
                        newUser.setLastName(lastName);
                        newUser.setProfilePictureUrl(picture);
                        newUser.setGoogleId(googleId);
                        newUser.setRole(Role.STAFF); // Default role
                        newUser.setEnabled(true);
                        return userRepository.save(newUser);
                    });

            // 4️⃣ Update avatar if changed
            if (picture != null && !picture.equals(user.getProfilePictureUrl())) {
                user.setProfilePictureUrl(picture);
                user.setGoogleId(googleId);
                userRepository.save(user);
            }

            // 5️⃣ Generate JWT token
            String jwt = jwtHandler.generateToken(
                    user.getId().toString(),
                    user.getEmail(),
                    user.getRole().name()
            );

            AuthResponse authResponse = new AuthResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRole().name(),
                    user.getProfilePictureUrl(),
                    user.getTwoFaEnabled(),
                    false,
                    "Login with Google successful"
            );

            authResponse.setAccessToken(jwt);
            authResponse.setExpiresIn(jwtExpiration);

            return authResponse;

        } catch (Exception e) {
            throw new RuntimeException("Google login failed: " + e.getMessage());
        }
    }


    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
}
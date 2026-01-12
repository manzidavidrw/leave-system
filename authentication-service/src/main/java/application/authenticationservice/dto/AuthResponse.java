package application.authenticationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String profilePictureUrl;
    private Boolean twoFaEnabled;
    private Boolean requires2FA;
    private String message;

    // JWT token
    private String accessToken;
    private Long expiresIn; // Token expiration time in milliseconds



    // Constructor without tokens (for 2FA and error responses)
    public AuthResponse(Long userId, String email, String firstName, String lastName,
                        String role, String profilePictureUrl, Boolean twoFaEnabled,
                        Boolean requires2FA, String message) {
        this.userId = userId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.profilePictureUrl = profilePictureUrl;
        this.twoFaEnabled = twoFaEnabled;
        this.requires2FA = requires2FA;
        this.message = message;
    }

}
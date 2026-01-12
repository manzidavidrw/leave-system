package application.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 2FA Verification Request
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFAVerificationRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotBlank(message = "2FA code is required")
    private String code;
}
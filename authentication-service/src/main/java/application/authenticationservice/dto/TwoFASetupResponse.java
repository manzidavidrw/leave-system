package application.authenticationservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 2FA Setup Response
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TwoFASetupResponse {
    private String secret;
    private String qrCodeUrl;
    private String message;
}
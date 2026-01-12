package application.authenticationservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// Google Profile Request
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleProfileRequest {

    @NotBlank(message = "Google access token is required")
    private String accessToken;


}

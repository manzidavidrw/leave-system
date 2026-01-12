package application.leavemanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveActionDTO {

    @NotBlank(message = "Action is required (APPROVE or REJECT)")
    private String action;

    private String comments;
}

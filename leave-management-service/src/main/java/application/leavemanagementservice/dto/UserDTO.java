package application.leavemanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String role; // EMPLOYEE, MANAGER, HR_ADMIN
    private String department;
    private Long managerId; // ID of their manager
    private LocalDate joiningDate;
}
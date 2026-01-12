package application.leavemanagementservice.dto;

import application.leavemanagementservice.ENUM.LeaveStatus;
import application.leavemanagementservice.ENUM.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestResponse {
    private Long id;
    private Long userId;
    private String employeeName;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double numberOfDays;
    private LeaveStatus status;
    private String reason;
    private Long managerId;
    private String managerName;
    private String managerComments;
    private String documentUrl;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
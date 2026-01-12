package application.leavemanagementservice.dto;

import application.leavemanagementservice.ENUM.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveCalendarDTO {
    private Long id;
    private Long userId;
    private String employeeName;
    private String department;
    private LeaveType leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double numberOfDays;
    private String status;
}
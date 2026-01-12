package application.leavemanagementservice.dto;

import application.leavemanagementservice.ENUM.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalanceResponse {
    private Long id;
    private Long userId;
    private LeaveType leaveType;
    private String leaveTypeName;
    private Integer year;
    private Double totalDays;
    private Double usedDays;
    private Double availableDays;
    private Double accruedDays;           // Days earned so far
    private Double carryoverDays;         // Days from last year
    private LocalDate carryoverExpiryDate; // When carryover expires
    private Boolean carryoverExpired;     // Whether carryover has expired
    private String accrualInfo;
}
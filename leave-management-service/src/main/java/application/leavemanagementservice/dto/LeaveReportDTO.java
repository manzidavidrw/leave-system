package application.leavemanagementservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveReportDTO {
    private Long userId;
    private String employeeName;
    private String leaveType;
    private Double totalDays;
    private Double usedDays;
    private Double availableDays;
    private Integer pendingRequests;
    private Integer approvedRequests;
    private Integer rejectedRequests;
}

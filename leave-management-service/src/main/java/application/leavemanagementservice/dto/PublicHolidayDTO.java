package application.leavemanagementservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicHolidayDTO {
    private Long id;
    private String name;
    private LocalDate date;
    private String description;
    private Boolean isRecurring;
}



package application.leavemanagementservice.Controller;

import application.leavemanagementservice.Service.CalendarService;
import application.leavemanagementservice.dto.EmployeeOnLeaveDTO;
import application.leavemanagementservice.dto.LeaveCalendarDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/my-calendar")
    public ResponseEntity<List<LeaveCalendarDTO>> getMyLeaveCalendar(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<LeaveCalendarDTO> calendar = calendarService.getLeaveCalendar(userId, startDate, endDate);
        return ResponseEntity.ok(calendar);
    }
    @GetMapping("/on-leave-today")
    public ResponseEntity<List<EmployeeOnLeaveDTO>> getEmployeesOnLeaveToday() {
        List<EmployeeOnLeaveDTO> employees = calendarService.getEmployeesOnLeaveToday();
        return ResponseEntity.ok(employees);
    }
}
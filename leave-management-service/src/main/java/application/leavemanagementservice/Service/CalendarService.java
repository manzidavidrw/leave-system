package application.leavemanagementservice.Service;

import application.leavemanagementservice.Entity.LeaveRequest;
import application.leavemanagementservice.Entity.PublicHoliday;
import application.leavemanagementservice.ENUM.LeaveStatus;
import application.leavemanagementservice.Repository.LeaveRequestRepository;
import application.leavemanagementservice.Repository.PublicHolidayRepository;
import application.leavemanagementservice.config.AuthServiceClient;
import application.leavemanagementservice.dto.EmployeeOnLeaveDTO;
import application.leavemanagementservice.dto.LeaveCalendarDTO;
import application.leavemanagementservice.dto.PublicHolidayDTO;
import application.leavemanagementservice.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CalendarService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final PublicHolidayRepository publicHolidayRepository;
    private final AuthServiceClient authServiceClient;

    public List<LeaveCalendarDTO> getLeaveCalendar(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching leave calendar for user {} from {} to {}", userId, startDate, endDate);

        List<LeaveRequest> leaves = leaveRequestRepository.findByUserId(userId).stream()
                .filter(leave -> {
                    return (leave.getStatus() == LeaveStatus.APPROVED || leave.getStatus() == LeaveStatus.PENDING) &&
                            !leave.getEndDate().isBefore(startDate) &&
                            !leave.getStartDate().isAfter(endDate);
                })
                .collect(Collectors.toList());

        UserDTO user = authServiceClient.getUserById(userId);

        return leaves.stream()
                .map(leave -> mapToCalendarDTO(leave, user))
                .collect(Collectors.toList());
    }

    public List<LeaveCalendarDTO> getTeamLeaveCalendar(LocalDate startDate, LocalDate endDate) {
        log.info("Fetching team leave calendar from {} to {}", startDate, endDate);

        List<LeaveRequest> leaves = leaveRequestRepository.findAll().stream()
                .filter(leave -> {
                    return leave.getStatus() == LeaveStatus.APPROVED &&
                            !leave.getEndDate().isBefore(startDate) &&
                            !leave.getStartDate().isAfter(endDate);
                })
                .collect(Collectors.toList());

        return leaves.stream()
                .map(leave -> {
                    UserDTO user = authServiceClient.getUserById(leave.getUserId());
                    return mapToCalendarDTO(leave, user);
                })
                .collect(Collectors.toList());
    }

    public List<EmployeeOnLeaveDTO> getEmployeesOnLeaveToday() {
        LocalDate today = LocalDate.now();
        log.info("Fetching employees on leave today: {}", today);

        List<LeaveRequest> leavesToday = leaveRequestRepository.findAll().stream()
                .filter(leave -> leave.getStatus() == LeaveStatus.APPROVED &&
                        !leave.getStartDate().isAfter(today) &&
                        !leave.getEndDate().isBefore(today))
                .collect(Collectors.toList());

        return leavesToday.stream()
                .map(leave -> {
                    UserDTO user = authServiceClient.getUserById(leave.getUserId());
                    EmployeeOnLeaveDTO dto = new EmployeeOnLeaveDTO();
                    dto.setUserId(user.getId());
                    dto.setEmployeeName(user.getFirstName() + " " + user.getLastName());
                    dto.setDepartment(user.getDepartment());
                    dto.setLeaveType(leave.getLeaveType().getDisplayName());
                    dto.setStartDate(leave.getStartDate());
                    dto.setEndDate(leave.getEndDate());
                    dto.setNumberOfDays(leave.getNumberOfDays());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<PublicHolidayDTO> getUpcomingHolidays(int months) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusMonths(months);

        log.info("Fetching upcoming holidays from {} to {}", startDate, endDate);

        List<PublicHoliday> holidays = publicHolidayRepository.findHolidaysBetween(startDate, endDate);

        return holidays.stream()
                .map(this::mapToHolidayDTO)
                .collect(Collectors.toList());
    }

    public List<PublicHolidayDTO> getHolidaysByYear(Integer year) {
        log.info("Fetching holidays for year: {}", year);

        List<PublicHoliday> holidays = publicHolidayRepository.findByYear(year);

        return holidays.stream()
                .map(this::mapToHolidayDTO)
                .collect(Collectors.toList());
    }

    private LeaveCalendarDTO mapToCalendarDTO(LeaveRequest leave, UserDTO user) {
        LeaveCalendarDTO dto = new LeaveCalendarDTO();
        dto.setId(leave.getId());
        dto.setUserId(leave.getUserId());
        dto.setEmployeeName(user.getFirstName() + " " + user.getLastName());
        dto.setDepartment(user.getDepartment());
        dto.setLeaveType(leave.getLeaveType());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setNumberOfDays(leave.getNumberOfDays());
        dto.setStatus(leave.getStatus().toString());
        return dto;
    }

    private PublicHolidayDTO mapToHolidayDTO(PublicHoliday holiday) {
        PublicHolidayDTO dto = new PublicHolidayDTO();
        dto.setId(holiday.getId());
        dto.setName(holiday.getName());
        dto.setDate(holiday.getDate());
        dto.setDescription(holiday.getDescription());
        dto.setIsRecurring(holiday.getIsRecurring());
        return dto;
    }
}
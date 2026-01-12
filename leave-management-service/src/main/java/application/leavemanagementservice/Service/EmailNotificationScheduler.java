package application.leavemanagementservice.Service;

import application.leavemanagementservice.Entity.LeaveRequest;
import application.leavemanagementservice.ENUM.LeaveStatus;
import application.leavemanagementservice.Repository.LeaveRequestRepository;
import application.leavemanagementservice.config.AuthServiceClient;
import application.leavemanagementservice.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationScheduler {

    private final LeaveRequestRepository leaveRequestRepository;
    private final AuthServiceClient authServiceClient;
    private final EmailService emailService;

    // Run every day at 9 AM
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendUpcomingLeaveReminders() {
        log.info("Running scheduled task: Send upcoming leave reminders");

        LocalDate today = LocalDate.now();
        LocalDate threeDaysFromNow = today.plusDays(3);

        List<LeaveRequest> upcomingLeaves = leaveRequestRepository.findAll().stream()
                .filter(leave -> leave.getStatus() == LeaveStatus.APPROVED &&
                        leave.getStartDate().equals(threeDaysFromNow))
                .toList();

        for (LeaveRequest leave : upcomingLeaves) {
            try {
                UserDTO employee = authServiceClient.getUserById(leave.getUserId());
                emailService.sendUpcomingLeaveReminder(employee, leave, 3);
            } catch (Exception e) {
                log.error("Failed to send reminder for leave request {}: {}", leave.getId(), e.getMessage());
            }
        }

        log.info("Sent {} upcoming leave reminders", upcomingLeaves.size());
    }

    // Run every day at 10 AM to notify managers about pending approvals
    @Scheduled(cron = "0 0 10 * * ?")
    public void notifyManagersOfPendingApprovals() {
        log.info("Running scheduled task: Notify managers of pending approvals");

        List<LeaveRequest> pendingLeaves = leaveRequestRepository.findByStatus(LeaveStatus.PENDING);

        // Group by manager and send consolidated email
        // For now, we'll skip this as managers can check the system
        // But you can implement a consolidated daily digest here

        log.info("Found {} pending leave requests requiring approval", pendingLeaves.size());
    }
}
package application.leavemanagementservice.Service;

import application.leavemanagementservice.Entity.LeaveRequest;
import application.leavemanagementservice.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    @Value("${brevo.sender.name}")
    private String senderName;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    @Async
    public void sendLeaveSubmissionEmail(UserDTO employee, LeaveRequest leaveRequest) {
        sendEmail(employee.getEmail(),
                "Leave Request Submitted - " + leaveRequest.getId(),
                buildLeaveSubmissionHtml(employee, leaveRequest));
    }

    private String buildLeaveSubmissionHtml(UserDTO employee, LeaveRequest leaveRequest) {
        return "<html><body>" +
                "<h3>Hello " + employee.getFirstName() + ",</h3>" +
                "<p>Your leave request has been submitted successfully.</p>" +
                "<ul>" +
                "<li>Leave Type: " + leaveRequest.getLeaveType().getDisplayName() + "</li>" +
                "<li>Start Date: " + leaveRequest.getStartDate() + "</li>" +
                "<li>End Date: " + leaveRequest.getEndDate() + "</li>" +
                "<li>Number of Days: " + leaveRequest.getNumberOfDays() + "</li>" +
                "<li>Status: PENDING</li>" +
                "</ul>" +
                "<p>HR Team</p>" +
                "</body></html>";
    }

    // ================== LEAVE APPROVAL ==================
    @Async
    public void sendLeaveApprovalEmail(UserDTO employee, LeaveRequest leaveRequest, UserDTO manager) {
        sendEmail(employee.getEmail(),
                "Leave Request Approved - " + leaveRequest.getId(),
                buildLeaveApprovalHtml(employee, leaveRequest, manager));
    }

    private String buildLeaveApprovalHtml(UserDTO employee, LeaveRequest leaveRequest, UserDTO manager) {
        return "<html><body>" +
                "<h3>Hello " + employee.getFirstName() + ",</h3>" +
                "<p>Your leave request has been <strong>APPROVED</strong> by " +
                manager.getFirstName() + " " + manager.getLastName() + ".</p>" +
                "<ul>" +
                "<li>Leave Type: " + leaveRequest.getLeaveType().getDisplayName() + "</li>" +
                "<li>Start Date: " + leaveRequest.getStartDate() + "</li>" +
                "<li>End Date: " + leaveRequest.getEndDate() + "</li>" +
                "<li>Number of Days: " + leaveRequest.getNumberOfDays() + "</li>" +
                "<li>Manager Comments: " + (leaveRequest.getManagerComments() != null ? leaveRequest.getManagerComments() : "None") + "</li>" +
                "</ul>" +
                "<p>Enjoy your leave!</p>" +
                "<p>HR Team</p>" +
                "</body></html>";
    }

    // ================== LEAVE REJECTION ==================
    @Async
    public void sendLeaveRejectionEmail(UserDTO employee, LeaveRequest leaveRequest, UserDTO manager) {
        sendEmail(employee.getEmail(),
                "Leave Request Rejected - " + leaveRequest.getId(),
                buildLeaveRejectionHtml(employee, leaveRequest, manager));
    }

    private String buildLeaveRejectionHtml(UserDTO employee, LeaveRequest leaveRequest, UserDTO manager) {
        return "<html><body>" +
                "<h3>Hello " + employee.getFirstName() + ",</h3>" +
                "<p>Your leave request has been <strong>REJECTED</strong> by " +
                manager.getFirstName() + " " + manager.getLastName() + ".</p>" +
                "<ul>" +
                "<li>Leave Type: " + leaveRequest.getLeaveType().getDisplayName() + "</li>" +
                "<li>Start Date: " + leaveRequest.getStartDate() + "</li>" +
                "<li>End Date: " + leaveRequest.getEndDate() + "</li>" +
                "<li>Number of Days: " + leaveRequest.getNumberOfDays() + "</li>" +
                "<li>Reason: " + (leaveRequest.getManagerComments() != null ? leaveRequest.getManagerComments() : "Not specified") + "</li>" +
                "</ul>" +
                "<p>Please contact your manager for more details.</p>" +
                "<p>HR Team</p>" +
                "</body></html>";
    }

    // ================== UPCOMING LEAVE REMINDER ==================
    @Async
    public void sendUpcomingLeaveReminder(UserDTO employee, LeaveRequest leaveRequest, int daysUntilLeave) {
        sendEmail(employee.getEmail(),
                "Reminder: Upcoming Leave in " + daysUntilLeave + " days",
                buildUpcomingLeaveReminderHtml(employee, leaveRequest, daysUntilLeave));
    }

    private String buildUpcomingLeaveReminderHtml(UserDTO employee, LeaveRequest leaveRequest, int daysUntilLeave) {
        return "<html><body>" +
                "<h3>Hello " + employee.getFirstName() + ",</h3>" +
                "<p>This is a reminder that your leave is starting in <strong>" + daysUntilLeave + "</strong> days.</p>" +
                "<ul>" +
                "<li>Leave Type: " + leaveRequest.getLeaveType().getDisplayName() + "</li>" +
                "<li>Start Date: " + leaveRequest.getStartDate() + "</li>" +
                "<li>End Date: " + leaveRequest.getEndDate() + "</li>" +
                "<li>Number of Days: " + leaveRequest.getNumberOfDays() + "</li>" +
                "</ul>" +
                "<p>Please ensure you complete any pending tasks before your leave.</p>" +
                "<p>HR Team</p>" +
                "</body></html>";
    }

    // ================== GENERIC SEND EMAIL METHOD ==================
    private void sendEmail(String recipientEmail, String subject, String htmlContent) {
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", senderName, "email", senderEmail));
            body.put("to", List.of(Map.of("email", recipientEmail)));
            body.put("subject", subject);
            body.put("htmlContent", htmlContent);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            RestTemplate restTemplate = new RestTemplate();
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    BREVO_API_URL,
                    HttpMethod.POST,
                    request,
                    String.class
            );

            log.info("Email sent to {} | Subject: {} | Response: {}", recipientEmail, subject, response.getBody());
        } catch (Exception e) {
            log.error("Failed to send email to {} | Subject: {} | Error: {}", recipientEmail, subject, e.getMessage());
        }
    }
}

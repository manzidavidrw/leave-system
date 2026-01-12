package application.leavemanagementservice.Controller;


import application.leavemanagementservice.Service.LeaveService;
import application.leavemanagementservice.dto.LeaveActionDTO;
import application.leavemanagementservice.dto.LeaveBalanceResponse;
import application.leavemanagementservice.dto.LeaveRequestDTO;
import application.leavemanagementservice.dto.LeaveRequestResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;
    @Autowired
    private Validator validator;
    private final ObjectMapper objectMapper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LeaveRequestResponse> createLeaveRequest(
            @RequestParam("data") String dtoJson,
            @RequestPart(value = "document", required = false) MultipartFile document,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Authorization") String token) {

        try {
            // Parse JSON string to DTO
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            LeaveRequestDTO dto = objectMapper.readValue(dtoJson, LeaveRequestDTO.class);

            // Manually validate
            Set<ConstraintViolation<LeaveRequestDTO>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                // Collect validation error messages
                Map<String, String> errors = new HashMap<>();
                for (ConstraintViolation<LeaveRequestDTO> violation : violations) {
                    errors.put(violation.getPropertyPath().toString(), violation.getMessage());
                }
                throw new ValidationException("Validation failed: " + errors);
            }

            LeaveRequestResponse response = leaveService.createLeaveRequest(dto, userId, token, document);
            return new ResponseEntity<>(response, HttpStatus.CREATED);

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for data field: " + e.getMessage());
        }
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<LeaveRequestResponse>> getMyLeaveRequests(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Authorization") String token) {

        List<LeaveRequestResponse> requests = leaveService.getMyLeaveRequests(userId, token);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/my-balances")
    public ResponseEntity<List<LeaveBalanceResponse>> getMyLeaveBalances(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Authorization") String token) {

        List<LeaveBalanceResponse> balances = leaveService.getMyLeaveBalances(userId, token);
        return ResponseEntity.ok(balances);
    }

    @DeleteMapping("/{requestId}/cancel")
    public ResponseEntity<Void> cancelLeaveRequest(
            @PathVariable Long requestId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("Authorization") String token) {

        leaveService.cancelLeaveRequest(requestId, userId, token);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/pending-approvals")
    public ResponseEntity<?> getPendingApprovals(
            @RequestHeader("X-User-Id") Long managerId,
            @RequestHeader("Authorization") String token) {

        List<LeaveRequestResponse> requests = leaveService.getPendingApprovals(managerId, token);

        if (requests.isEmpty()) {
            return ResponseEntity.ok("No pending approvals");
        }

        return ResponseEntity.ok(requests);
    }

    @PostMapping("/{requestId}/review")
    public ResponseEntity<LeaveRequestResponse> reviewLeaveRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody LeaveActionDTO action,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestHeader("X-User-Role") String role) {

        LeaveRequestResponse response =
                leaveService.reviewLeaveRequest(requestId, action, adminId, role);

        return ResponseEntity.ok(response);
    }

    @GetMapping()
    public ResponseEntity<List<LeaveRequestResponse>> getAllLeaves() {
        List<LeaveRequestResponse> leaves = leaveService.getAllLeaves();
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/{leaveId}")
    public ResponseEntity<LeaveRequestResponse> getLeaveById(
            @PathVariable Long leaveId
    ) {
        LeaveRequestResponse leave = leaveService.getLeaveById(leaveId);
        return ResponseEntity.ok(leave);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<LeaveRequestResponse>> getLeaveByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(leaveService.getLeavesByUser(userId));
    }


}

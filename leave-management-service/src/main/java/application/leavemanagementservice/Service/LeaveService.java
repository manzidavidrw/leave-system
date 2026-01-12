package application.leavemanagementservice.Service;

import application.leavemanagementservice.Entity.LeaveBalance;
import application.leavemanagementservice.Entity.LeaveRequest;
import application.leavemanagementservice.Repository.LeaveBalanceRepository;
import application.leavemanagementservice.Repository.LeaveRequestRepository;
import application.leavemanagementservice.ENUM.LeaveStatus;
import application.leavemanagementservice.ENUM.LeaveType;
import application.leavemanagementservice.config.AuthServiceClient;
import application.leavemanagementservice.dto.LeaveActionDTO;
import application.leavemanagementservice.dto.LeaveBalanceResponse;
import application.leavemanagementservice.dto.LeaveRequestDTO;
import application.leavemanagementservice.dto.LeaveRequestResponse;
import application.leavemanagementservice.dto.UserDTO;
import application.leavemanagementservice.exceptions.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AuthServiceClient authServiceClient;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryservice;
    private final LeaveAccrualService leaveAccrualService;

    @Transactional
    public LeaveRequestResponse createLeaveRequest(
            LeaveRequestDTO dto,
            Long userId,
            String token,
            MultipartFile document
    ) {
        log.info("Creating leave request for user: {}", userId);

        UserDTO user = authServiceClient.getUserById(userId);

        double numberOfDays = calculateWorkingDays(dto.getStartDate(), dto.getEndDate());

        List<LeaveRequest> overlapping =
                leaveRequestRepository.findOverlappingLeaves(
                        userId, dto.getStartDate(), dto.getEndDate()
                );

        if (!overlapping.isEmpty()) {
            throw new LeaveOverlapException("You have overlapping leave requests for these dates");
        }

        int year = dto.getStartDate().getYear();
        LeaveBalance balance = leaveBalanceRepository
                .findByUserIdAndLeaveTypeAndYear(userId, dto.getLeaveType(), year)
                .orElseGet(() -> createDefaultBalance(userId, dto.getLeaveType(), year, user.getJoiningDate()));

        if (balance.getAvailableDays() < numberOfDays) {
            throw new InsufficientLeaveBalanceException(
                    String.format(
                            "Insufficient leave balance. Available: %.1f days, Requested: %.1f days",
                            balance.getAvailableDays(), numberOfDays
                    )
            );
        }

        LeaveRequest leaveRequest = new LeaveRequest();
        leaveRequest.setUserId(userId);
        leaveRequest.setLeaveType(dto.getLeaveType());
        leaveRequest.setStartDate(dto.getStartDate());
        leaveRequest.setEndDate(dto.getEndDate());
        leaveRequest.setNumberOfDays(numberOfDays);
        leaveRequest.setReason(dto.getReason());
        leaveRequest.setStatus(LeaveStatus.PENDING);
        leaveRequest.setManagerId(user.getManagerId());

        // 🔹 DOCUMENT UPLOAD (OPTIONAL)
        if (document != null && !document.isEmpty()) {
            try {
                Map<String, Object> uploadResult = cloudinaryservice.uploadFile(document, "leave-documents");
                String documentUrl = (String) uploadResult.get("secure_url");
                leaveRequest.setDocumentUrl(documentUrl);
                log.info("Document uploaded successfully: {}", documentUrl);
            } catch (FileUploadException e) {
                log.error("Failed to upload document for leave request: {}", e.getMessage());
                throw e; // Re-throw to rollback transaction
            } catch (IOException e) {
                log.error("IO error while uploading document: {}", e.getMessage());
                throw new FileUploadException("Failed to upload document due to IO error");
            }
        }

        leaveRequest = leaveRequestRepository.save(leaveRequest);

        log.info("Leave request created successfully with ID: {}", leaveRequest.getId());

        return mapToResponse(leaveRequest, user, null);
    }


    @Transactional
    public LeaveRequestResponse reviewLeaveRequest(Long requestId, LeaveActionDTO action,
                                                   Long managerId, String token) {
        log.info("Manager {} reviewing leave request {}", managerId, requestId);

        application.leavemanagementservice.Entity.LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        // Get manager details and verify they have MANAGER or HR_ADMIN role
        UserDTO manager = authServiceClient.getUserById(managerId);
        if (!"MANAGER".equals(manager.getRole()) && !"ADMIN".equals(manager.getRole())) {
            throw new UnauthorizedException("Only managers or HR admins can review leave requests");
        }

        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException("Leave request has already been reviewed");
        }

        // Get employee details
        UserDTO employee = authServiceClient.getUserById(leaveRequest.getUserId());

        // Update leave request status
        if ("APPROVE".equalsIgnoreCase(action.getAction())) {
            leaveRequest.setStatus(LeaveStatus.APPROVED);

            // Deduct from balance
            int year = leaveRequest.getStartDate().getYear();
            application.leavemanagementservice.Entity.LeaveBalance balance = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeAndYear(leaveRequest.getUserId(),
                            leaveRequest.getLeaveType(), year)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

            balance.deductDays(leaveRequest.getNumberOfDays());
            leaveBalanceRepository.save(balance);

            emailService.sendLeaveApprovalEmail(employee, leaveRequest, manager);

        } else if ("REJECT".equalsIgnoreCase(action.getAction())) {
            leaveRequest.setStatus(LeaveStatus.REJECTED);

            emailService.sendLeaveApprovalEmail(employee, leaveRequest, manager);

        } else {
            throw new IllegalArgumentException("Invalid action. Must be APPROVE or REJECT");
        }

        // Set the manager who approved/rejected
        leaveRequest.setManagerId(managerId);
        leaveRequest.setManagerComments(action.getComments());
        leaveRequest.setReviewedAt(LocalDateTime.now());

        leaveRequest = leaveRequestRepository.save(leaveRequest);

        log.info("Leave request {} has been {} by manager {}", requestId, leaveRequest.getStatus(), managerId);

        return mapToResponse(leaveRequest, employee, manager);
    }

    @Transactional
    public void cancelLeaveRequest(Long requestId, Long userId, String token) {
        log.info("User {} cancelling leave request {}", userId, requestId);

        application.leavemanagementservice.Entity.LeaveRequest leaveRequest = leaveRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request not found"));

        if (!leaveRequest.getUserId().equals(userId)) {
            throw new UnauthorizedException("You can only cancel your own leave requests");
        }

        if (leaveRequest.getStatus() == LeaveStatus.CANCELLED) {
            throw new IllegalStateException("Leave request is already cancelled");
        }

        LeaveStatus oldStatus = leaveRequest.getStatus();
        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(leaveRequest);

        // If it was approved, add days back to balance
        if (oldStatus == LeaveStatus.APPROVED) {
            int year = leaveRequest.getStartDate().getYear();
            application.leavemanagementservice.Entity.LeaveBalance balance = leaveBalanceRepository
                    .findByUserIdAndLeaveTypeAndYear(userId, leaveRequest.getLeaveType(), year)
                    .orElseThrow(() -> new ResourceNotFoundException("Leave balance not found"));

            balance.addBackDays(leaveRequest.getNumberOfDays());
            leaveBalanceRepository.save(balance);
        }

        log.info("Leave request {} cancelled successfully", requestId);
    }

    public List<LeaveRequestResponse> getMyLeaveRequests(Long userId, String token) {
        UserDTO user = authServiceClient.getUserById(userId);
        List<application.leavemanagementservice.Entity.LeaveRequest> requests = leaveRequestRepository.findByUserId(userId);

        return requests.stream()
                .map(req -> mapToResponse(req, user, null))
                .collect(Collectors.toList());
    }

    public List<LeaveRequestResponse> getTeamLeaveRequests(Long managerId, String token) {
        UserDTO manager = authServiceClient.getUserById(managerId);

        // Verify user is a manager or HR admin
        if (!"MANAGER".equals(manager.getRole()) && !"HR_ADMIN".equals(manager.getRole())) {
            throw new UnauthorizedException("Only managers or HR admins can view team leave requests");
        }

        // Get all leave requests (not just assigned to this manager)
        List<application.leavemanagementservice.Entity.LeaveRequest> requests = leaveRequestRepository.findAll();

        return requests.stream()
                .map(req -> {
                    UserDTO employee = authServiceClient.getUserById(req.getUserId());
                    UserDTO approver = req.getManagerId() != null ?
                            authServiceClient.getUserById(req.getManagerId()) : null;
                    return mapToResponse(req, employee, approver);
                })
                .collect(Collectors.toList());
    }

    public List<LeaveRequestResponse> getPendingApprovals(Long managerId, String token) {
        UserDTO manager = authServiceClient.getUserById(managerId);

        // Verify user is a manager or HR admin
        if (!"MANAGER".equals(manager.getRole()) && !"HR_ADMIN".equals(manager.getRole())) {
            throw new UnauthorizedException("Only managers or HR admins can view pending approvals");
        }

        // Get all pending requests (not just for this manager)
        List<application.leavemanagementservice.Entity.LeaveRequest> requests = leaveRequestRepository
                .findByStatus(LeaveStatus.PENDING);

        return requests.stream()
                .map(req -> {
                    UserDTO employee = authServiceClient.getUserById(req.getUserId());
                    return mapToResponse(req, employee, manager);
                })
                .collect(Collectors.toList());
    }

    public List<LeaveBalanceResponse> getMyLeaveBalances(Long userId, String token) {
        UserDTO user = authServiceClient.getUserById(userId);

        int currentYear = LocalDate.now().getYear();
        List<LeaveBalance> balances = leaveBalanceRepository.findByUserIdAndYear(userId, currentYear);

        // If no balances exist, create default ones
        if (balances.isEmpty()) {
            for (LeaveType type : LeaveType.values()) {
                balances.add(createDefaultBalance(userId, type, currentYear, user.getJoiningDate()));
            }
        }

        // Update accrued days for all balances
        balances = updateAccruedDays(balances, user.getJoiningDate());

        return balances.stream()
                .map(this::mapToBalanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Update accrued days for all balances in real-time
     */
    private List<LeaveBalance> updateAccruedDays(List<LeaveBalance> balances, LocalDate joiningDate) {
        int currentYear = LocalDate.now().getYear();

        for (LeaveBalance balance : balances) {
            // Only apply accrual to ANNUAL leave type
            if (balance.getLeaveType() == LeaveType.ANNUAL) {
                // Calculate accrued days
                double accruedDays = leaveAccrualService.calculateAccruedDays(
                        joiningDate != null ? joiningDate : LocalDate.of(currentYear, 1, 1),
                        currentYear
                );
                balance.setAccruedDays(accruedDays);

                // Check if carryover has expired
                if (balance.getCarryoverExpiryDate() != null &&
                        leaveAccrualService.hasCarryoverExpired(balance.getCarryoverExpiryDate())) {
                    balance.setCarryoverDays(0.0);
                }

                // Calculate total available
                double totalAvailable = leaveAccrualService.calculateTotalAvailable(
                        accruedDays,
                        balance.getCarryoverDays() != null ? balance.getCarryoverDays() : 0.0,
                        balance.getUsedDays(),
                        balance.getCarryoverExpiryDate() != null &&
                                leaveAccrualService.hasCarryoverExpired(balance.getCarryoverExpiryDate())
                );

                balance.setAvailableDays(totalAvailable);
                balance.setTotalDays(accruedDays + (balance.getCarryoverDays() != null ? balance.getCarryoverDays() : 0.0));

                leaveBalanceRepository.save(balance);
            }
        }

        return balances;
    }

    private LeaveBalance createDefaultBalance(Long userId, LeaveType leaveType, int year, LocalDate joiningDate) {
        LeaveBalance balance = new LeaveBalance();
        balance.setUserId(userId);
        balance.setLeaveType(leaveType);
        balance.setYear(year);
        balance.setUsedDays(0.0);
        balance.setEmploymentStartDate(joiningDate);

        if (leaveType == LeaveType.ANNUAL) {
            // Calculate accrued days based on joining date
            double accruedDays = leaveAccrualService.calculateAccruedDays(
                    joiningDate != null ? joiningDate : LocalDate.of(year, 1, 1),
                    year
            );

            balance.setAccruedDays(accruedDays);
            balance.setCarryoverDays(0.0);
            balance.setCarryoverExpiryDate(leaveAccrualService.getCarryoverExpiryDate(year));
            balance.setTotalDays(accruedDays);
            balance.setAvailableDays(accruedDays);
        } else {
            // Other leave types use fixed allowance
            balance.setAccruedDays((double) leaveType.getDefaultAllowance());
            balance.setTotalDays((double) leaveType.getDefaultAllowance());
            balance.setAvailableDays((double) leaveType.getDefaultAllowance());
        }

        return leaveBalanceRepository.save(balance);
    }

    private double calculateWorkingDays(LocalDate start, LocalDate end) {
        // Simple calculation - count all days (you can enhance to exclude weekends/holidays)
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    private LeaveRequestResponse mapToResponse(application.leavemanagementservice.Entity.LeaveRequest request, UserDTO employee, UserDTO manager) {
        LeaveRequestResponse response = new LeaveRequestResponse();
        response.setId(request.getId());
        response.setUserId(request.getUserId());
        response.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
        response.setLeaveType(request.getLeaveType());
        response.setStartDate(request.getStartDate());
        response.setEndDate(request.getEndDate());
        response.setNumberOfDays(request.getNumberOfDays());
        response.setStatus(request.getStatus());
        response.setReason(request.getReason());
        response.setManagerId(request.getManagerId());
        if (manager != null) {
            response.setManagerName(manager.getFirstName() + " " + manager.getLastName());
        }
        response.setManagerComments(request.getManagerComments());
        response.setDocumentUrl(request.getDocumentUrl());
        response.setReviewedAt(request.getReviewedAt());
        response.setCreatedAt(request.getCreatedAt());
        return response;
    }

    private LeaveBalanceResponse mapToBalanceResponse(LeaveBalance balance) {
        LeaveBalanceResponse response = new LeaveBalanceResponse();
        response.setId(balance.getId());
        response.setUserId(balance.getUserId());
        response.setLeaveType(balance.getLeaveType());
        response.setLeaveTypeName(balance.getLeaveType().getDisplayName());
        response.setYear(balance.getYear());
        response.setTotalDays(balance.getTotalDays());
        response.setUsedDays(balance.getUsedDays());
        response.setAvailableDays(balance.getAvailableDays());

        // ✅ Add accrual information
        response.setAccruedDays(balance.getAccruedDays());
        response.setCarryoverDays(balance.getCarryoverDays());
        response.setCarryoverExpiryDate(balance.getCarryoverExpiryDate());

        if (balance.getCarryoverExpiryDate() != null) {
            response.setCarryoverExpired(
                    leaveAccrualService.hasCarryoverExpired(balance.getCarryoverExpiryDate())
            );
        }

        if (balance.getLeaveType() == LeaveType.ANNUAL) {
            response.setAccrualInfo("1.66 days/month (20 days/year)");
        }

        return response;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getAllLeaves() {
        log.info("Fetching all leave requests in the system");

        List<LeaveRequest> leaves = leaveRequestRepository.findAll();

        return leaves.stream()
                .map(leave -> {
                    UserDTO user = authServiceClient.getUserById(leave.getUserId());
                    return mapToResponse(leave, user, null);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public LeaveRequestResponse getLeaveById(Long leaveId) {
        log.info("Fetching leave request with ID: {}", leaveId);

        LeaveRequest leaveRequest = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new LeaveNotFoundException(
                        String.format("Leave request with ID %d not found", leaveId)
                ));

        UserDTO user = authServiceClient.getUserById(leaveRequest.getUserId());

        return mapToResponse(leaveRequest, user, null);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequestResponse> getLeavesByUser(Long userId) {
        log.info("Fetching all leave requests for user: {}", userId);

        UserDTO user = authServiceClient.getUserById(userId);

        List<LeaveRequest> leaves = leaveRequestRepository.findByUserId(userId);

        return leaves.stream()
                .map(leave -> mapToResponse(leave, user, null))
                .collect(Collectors.toList());
    }


}
package application.leavemanagementservice.Service;


import application.leavemanagementservice.Entity.LeaveBalance;
import application.leavemanagementservice.Entity.LeaveRequest;
import application.leavemanagementservice.ENUM.LeaveStatus;
import application.leavemanagementservice.Repository.LeaveBalanceRepository;
import application.leavemanagementservice.Repository.LeaveRequestRepository;
import application.leavemanagementservice.config.AuthServiceClient;
import application.leavemanagementservice.dto.LeaveReportDTO;
import application.leavemanagementservice.dto.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final AuthServiceClient authServiceClient;

    public byte[] generateLeaveReportCSV(Integer year) {
        List<LeaveReportDTO> reportData = generateLeaveReportData(year);

        StringBuilder csv = new StringBuilder();
        csv.append("Employee Name,Department,Leave Type,Total Days,Used Days,Available Days,Pending,Approved,Rejected\n");

        for (LeaveReportDTO data : reportData) {
            csv.append(String.format("%s,%s,%s,%.1f,%.1f,%.1f,%d,%d,%d\n",
                    data.getEmployeeName(),
                    data.getLeaveType(),
                    data.getTotalDays(),
                    data.getUsedDays(),
                    data.getAvailableDays(),
                    data.getPendingRequests(),
                    data.getApprovedRequests(),
                    data.getRejectedRequests()
            ));
        }

        return csv.toString().getBytes();
    }

    public byte[] generateLeaveReportExcel(Integer year) throws IOException {
        List<LeaveReportDTO> reportData = generateLeaveReportData(year);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Leave Report " + year);

            // Create header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"Employee Name", "Department", "Leave Type", "Total Days",
                    "Used Days", "Available Days", "Pending", "Approved", "Rejected"};

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create data rows
            int rowNum = 1;
            for (LeaveReportDTO data : reportData) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(data.getEmployeeName());
                row.createCell(2).setCellValue(data.getLeaveType());
                row.createCell(3).setCellValue(data.getTotalDays());
                row.createCell(4).setCellValue(data.getUsedDays());
                row.createCell(5).setCellValue(data.getAvailableDays());
                row.createCell(6).setCellValue(data.getPendingRequests());
                row.createCell(7).setCellValue(data.getApprovedRequests());
                row.createCell(8).setCellValue(data.getRejectedRequests());
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        }
    }

    private List<LeaveReportDTO> generateLeaveReportData(Integer year) {
        // Get all leave balances for the year
        List<LeaveBalance> balances = leaveBalanceRepository.findByYear(year);

        // Get all leave requests for the year
        List<LeaveRequest> requests = leaveRequestRepository.findAll().stream()
                .filter(req -> req.getStartDate().getYear() == year)
                .collect(Collectors.toList());

        // Group requests by user and leave type
        Map<String, Map<String, List<LeaveRequest>>> requestsByUserAndType = requests.stream()
                .collect(Collectors.groupingBy(
                        req -> req.getUserId() + "-" + req.getLeaveType(),
                        Collectors.groupingBy(req -> req.getStatus().toString())
                ));

        return balances.stream().map(balance -> {
            UserDTO user = authServiceClient.getUserById(balance.getUserId());

            String key = balance.getUserId() + "-" + balance.getLeaveType();
            Map<String, List<LeaveRequest>> statusMap = requestsByUserAndType.getOrDefault(key, Map.of());

            LeaveReportDTO report = new LeaveReportDTO();
            report.setUserId(balance.getUserId());
            report.setEmployeeName(user.getFirstName() + " " + user.getLastName());
            report.setLeaveType(balance.getLeaveType().getDisplayName());
            report.setTotalDays(balance.getTotalDays());
            report.setUsedDays(balance.getUsedDays());
            report.setAvailableDays(balance.getAvailableDays());
            report.setPendingRequests(statusMap.getOrDefault("PENDING", List.of()).size());
            report.setApprovedRequests(statusMap.getOrDefault("APPROVED", List.of()).size());
            report.setRejectedRequests(statusMap.getOrDefault("REJECTED", List.of()).size());

            return report;
        }).collect(Collectors.toList());
    }
}
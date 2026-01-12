package application.leavemanagementservice.Controller;

import application.leavemanagementservice.Service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/leave/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/leave-report/csv")
    public ResponseEntity<byte[]> downloadLeaveReportCSV(
            @RequestParam(required = false) Integer year) {

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        byte[] csvData = reportService.generateLeaveReportCSV(year);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "leave-report-" + year + ".csv");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/leave-report/excel")
    public ResponseEntity<byte[]> downloadLeaveReportExcel(
            @RequestParam(required = false) Integer year) throws IOException {

        if (year == null) {
            year = LocalDate.now().getYear();
        }

        byte[] excelData = reportService.generateLeaveReportExcel(year);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", "leave-report-" + year + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}

package application.leavemanagementservice.Controller;

import application.leavemanagementservice.Entity.PublicHoliday;
import application.leavemanagementservice.Repository.PublicHolidayRepository;
import application.leavemanagementservice.Service.CalendarService;
import application.leavemanagementservice.dto.PublicHolidayDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leave/holidays")
@RequiredArgsConstructor
public class HolidayController {

    private final CalendarService calendarService;
    private final PublicHolidayRepository publicHolidayRepository;

    @GetMapping("/upcoming")
    public ResponseEntity<List<PublicHolidayDTO>> getUpcomingHolidays(
            @RequestParam(defaultValue = "6") int months) {

        List<PublicHolidayDTO> holidays = calendarService.getUpcomingHolidays(months);
        return ResponseEntity.ok(holidays);
    }

    @GetMapping("/year/{year}")
    public ResponseEntity<List<PublicHolidayDTO>> getHolidaysByYear(
            @PathVariable Integer year) {

        List<PublicHolidayDTO> holidays = calendarService.getHolidaysByYear(year);
        return ResponseEntity.ok(holidays);
    }

    @PostMapping
    public ResponseEntity<PublicHolidayDTO> createHoliday(@Valid @RequestBody PublicHolidayDTO dto) {
        PublicHoliday holiday = new PublicHoliday();
        holiday.setName(dto.getName());
        holiday.setDate(dto.getDate());
        holiday.setYear(dto.getDate().getYear());
        holiday.setDescription(dto.getDescription());
        holiday.setIsRecurring(dto.getIsRecurring() != null ? dto.getIsRecurring() : false);

        holiday = publicHolidayRepository.save(holiday);

        return new ResponseEntity<>(mapToDTO(holiday), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PublicHolidayDTO> updateHoliday(
            @PathVariable Long id,
            @Valid @RequestBody PublicHolidayDTO dto) {

        PublicHoliday holiday = publicHolidayRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Holiday not found"));

        holiday.setName(dto.getName());
        holiday.setDate(dto.getDate());
        holiday.setYear(dto.getDate().getYear());
        holiday.setDescription(dto.getDescription());
        holiday.setIsRecurring(dto.getIsRecurring());

        holiday = publicHolidayRepository.save(holiday);

        return ResponseEntity.ok(mapToDTO(holiday));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHoliday(@PathVariable Long id) {
        publicHolidayRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PublicHolidayDTO mapToDTO(PublicHoliday holiday) {
        PublicHolidayDTO dto = new PublicHolidayDTO();
        dto.setId(holiday.getId());
        dto.setName(holiday.getName());
        dto.setDate(holiday.getDate());
        dto.setDescription(holiday.getDescription());
        dto.setIsRecurring(holiday.getIsRecurring());
        return dto;
    }
}



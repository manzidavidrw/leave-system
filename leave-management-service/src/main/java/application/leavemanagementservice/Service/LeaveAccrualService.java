package application.leavemanagementservice.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
@Slf4j
public class LeaveAccrualService {

    private static final double MONTHLY_ACCRUAL_RATE = 1.66; // days per month
    private static final double MAX_CARRYOVER_DAYS = 5.0;
    private static final int CARRYOVER_EXPIRY_DAY = 31; // Jan 31
    private static final int CARRYOVER_EXPIRY_MONTH = 1; // January

    /**
     * Calculate accrued leave days based on employment start date
     */
    public double calculateAccruedDays(LocalDate employmentStartDate, int year) {
        LocalDate startOfYear = LocalDate.of(year, 1, 1);
        LocalDate endOfYear = LocalDate.of(year, 12, 31);
        LocalDate today = LocalDate.now();

        // Determine the start date for accrual calculation
        LocalDate accrualStart = employmentStartDate.isAfter(startOfYear)
                ? employmentStartDate
                : startOfYear;

        // Determine the end date (today or end of year, whichever is earlier)
        LocalDate accrualEnd = today.isBefore(endOfYear) ? today : endOfYear;

        // If employment started after today, no accrual yet
        if (accrualStart.isAfter(accrualEnd)) {
            return 0.0;
        }

        // Calculate complete months worked
        long monthsWorked = ChronoUnit.MONTHS.between(accrualStart, accrualEnd.plusDays(1));

        // Calculate accrued days
        double accruedDays = monthsWorked * MONTHLY_ACCRUAL_RATE;

        // Cap at annual maximum (20 days for ANNUAL leave)
        double maxAnnualDays = 20.0; // 12 months * 1.66 ≈ 20 days

        return Math.min(accruedDays, maxAnnualDays);
    }

    /**
     * Calculate carryover from previous year
     */
    public double calculateCarryover(double previousYearAvailable) {
        // Maximum 5 days can be carried over
        return Math.min(previousYearAvailable, MAX_CARRYOVER_DAYS);
    }

    /**
     * Get carryover expiry date (Jan 31 of current year)
     */
    public LocalDate getCarryoverExpiryDate(int year) {
        return LocalDate.of(year, CARRYOVER_EXPIRY_MONTH, CARRYOVER_EXPIRY_DAY);
    }

    /**
     * Check if carryover has expired
     */
    public boolean hasCarryoverExpired(LocalDate expiryDate) {
        return LocalDate.now().isAfter(expiryDate);
    }

    /**
     * Get total available days (accrued + carryover - used)
     */
    public double calculateTotalAvailable(double accruedDays, double carryoverDays,
                                          double usedDays, boolean carryoverExpired) {
        double totalAccrued = accruedDays;

        // Add carryover only if not expired
        if (!carryoverExpired && carryoverDays > 0) {
            totalAccrued += carryoverDays;
        }

        return totalAccrued - usedDays;
    }
}

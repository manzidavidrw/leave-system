package application.leavemanagementservice.config;

import application.leavemanagementservice.ENUM.LeaveType;
import application.leavemanagementservice.Entity.LeaveBalance;
import application.leavemanagementservice.Repository.LeaveBalanceRepository;
import application.leavemanagementservice.Service.LeaveAccrualService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class YearEndCarryoverJob {

    private final LeaveBalanceRepository leaveBalanceRepository;
    private final LeaveAccrualService leaveAccrualService;

    /**
     * Run on December 31st at 11:59 PM to process carryovers
     */
    @Scheduled(cron = "0 59 23 31 12 *")
    public void processYearEndCarryover() {
        log.info("Starting year-end carryover process");

        int currentYear = LocalDate.now().getYear();
        int nextYear = currentYear + 1;

        List<LeaveBalance> currentYearBalances = leaveBalanceRepository.findByYear(currentYear);

        for (LeaveBalance balance : currentYearBalances) {
            if (balance.getLeaveType() == LeaveType.ANNUAL && balance.getAvailableDays() > 0) {
                // Calculate carryover (max 5 days)
                double carryover = leaveAccrualService.calculateCarryover(balance.getAvailableDays());

                if (carryover > 0) {
                    // Create next year's balance with carryover
                    LeaveBalance nextYearBalance = new LeaveBalance();
                    nextYearBalance.setUserId(balance.getUserId());
                    nextYearBalance.setLeaveType(LeaveType.ANNUAL);
                    nextYearBalance.setYear(nextYear);
                    nextYearBalance.setCarryoverDays(carryover);
                    nextYearBalance.setCarryoverExpiryDate(
                            leaveAccrualService.getCarryoverExpiryDate(nextYear)
                    );
                    nextYearBalance.setAccruedDays(0.0);
                    nextYearBalance.setUsedDays(0.0);
                    nextYearBalance.setTotalDays(carryover);
                    nextYearBalance.setAvailableDays(carryover);
                    nextYearBalance.setEmploymentStartDate(balance.getEmploymentStartDate());

                    leaveBalanceRepository.save(nextYearBalance);

                    log.info("Carried over {} days for user {} to year {}",
                            carryover, balance.getUserId(), nextYear);
                }
            }
        }

        log.info("Year-end carryover process completed");
    }
}
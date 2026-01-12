package application.leavemanagementservice.Entity;

import application.leavemanagementservice.ENUM.LeaveType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "leave_type", "year"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false)
    private LeaveType leaveType;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Double totalDays;

    @Column(nullable = false)
    private Double usedDays = 0.0;

    @Column(nullable = false)
    private Double availableDays;

    @Column(name = "accrued_days")
    private Double accruedDays;    // Days accrued so far this year

    @Column(name = "carryover_days")
    private Double carryoverDays;  // Days carried over from previous year

    @Column(name = "carryover_expiry_date")
    private LocalDate carryoverExpiryDate;  // Usually Jan 31 of current year

    @Column(name = "employment_start_date")
    private LocalDate employmentStartDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (availableDays == null) {
            availableDays = totalDays - usedDays;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        availableDays = totalDays - usedDays;
    }

    public void deductDays(double days) {
        this.usedDays += days;
        this.availableDays = this.totalDays - this.usedDays;
    }

    public void addBackDays(double days) {
        this.usedDays = Math.max(0, this.usedDays - days);
        this.availableDays = this.totalDays - this.usedDays;
    }
}
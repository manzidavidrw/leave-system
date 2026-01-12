package application.leavemanagementservice.Repository;

import application.leavemanagementservice.ENUM.LeaveStatus;
import application.leavemanagementservice.Entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByUserId(Long userId);

    List<LeaveRequest> findByUserIdAndStatus(Long userId, LeaveStatus status);

    List<LeaveRequest> findByManagerId(Long managerId);

    List<LeaveRequest> findByStatus(LeaveStatus status);

    @Query("SELECT lr FROM LeaveRequest lr WHERE lr.userId = :userId " +
            "AND lr.status IN (application.leavemanagementservice.ENUM.LeaveStatus.PENDING, application.leavemanagementservice.ENUM.LeaveStatus.APPROVED) " +
            "AND ((lr.startDate BETWEEN :startDate AND :endDate) " +
            "OR (lr.endDate BETWEEN :startDate AND :endDate) " +
            "OR (lr.startDate <= :startDate AND lr.endDate >= :endDate))")
    List<LeaveRequest> findOverlappingLeaves(@Param("userId") Long userId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}
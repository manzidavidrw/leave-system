package application.leavemanagementservice.Repository;

import application.leavemanagementservice.ENUM.LeaveType;
import application.leavemanagementservice.Entity.LeaveBalance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveBalanceRepository extends JpaRepository<LeaveBalance, Long> {

    Optional<LeaveBalance> findByUserIdAndLeaveTypeAndYear(Long userId, LeaveType leaveType, Integer year);

    List<LeaveBalance> findByUserIdAndYear(Long userId, Integer year);

    List<LeaveBalance> findByUserId(Long userId);
    List<LeaveBalance> findByYear(Integer year);

}

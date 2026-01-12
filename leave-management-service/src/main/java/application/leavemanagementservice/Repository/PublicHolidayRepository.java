package application.leavemanagementservice.Repository;


import application.leavemanagementservice.Entity.PublicHoliday;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PublicHolidayRepository extends JpaRepository<PublicHoliday, Long> {

    List<PublicHoliday> findByYear(Integer year);

    @Query("SELECT ph FROM PublicHoliday ph WHERE ph.date >= :startDate ORDER BY ph.date ASC")
    List<PublicHoliday> findUpcomingHolidays(@Param("startDate") LocalDate startDate);

    @Query("SELECT ph FROM PublicHoliday ph WHERE ph.date BETWEEN :startDate AND :endDate")
    List<PublicHoliday> findHolidaysBetween(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
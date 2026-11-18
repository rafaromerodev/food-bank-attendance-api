package dev.rafaromero.attendance.checkin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByBeneficiaryIdAndAttendedOn(Long beneficiaryId, LocalDate attendedOn);

    List<Attendance> findByBeneficiaryIdOrderByAttendedAtDescIdDesc(Long beneficiaryId);

    long countByAttendedOnBetween(LocalDate from, LocalDate to);

    @Query("select count(distinct a.beneficiary.id) from Attendance a where a.attendedOn between :from and :to")
    long countDistinctBeneficiariesBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);
}

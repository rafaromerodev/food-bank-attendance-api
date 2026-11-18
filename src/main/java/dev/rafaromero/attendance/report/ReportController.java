package dev.rafaromero.attendance.report;

import java.time.Clock;
import java.time.LocalDate;

import dev.rafaromero.attendance.beneficiary.BeneficiaryRepository;
import dev.rafaromero.attendance.checkin.AttendanceRepository;
import dev.rafaromero.attendance.common.InvalidInputException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Figures the food bank reports to funders for a period. */
@RestController
@Tag(name = "Reports")
class ReportController {

    private final AttendanceRepository attendances;
    private final BeneficiaryRepository beneficiaries;
    private final Clock clock;

    ReportController(AttendanceRepository attendances, BeneficiaryRepository beneficiaries, Clock clock) {
        this.attendances = attendances;
        this.beneficiaries = beneficiaries;
        this.clock = clock;
    }

    record Summary(LocalDate from, LocalDate to, long attendances, long peopleAttended,
            long newBeneficiaries, long activeWithExpiredFile) {
    }

    @GetMapping("/api/reports/summary")
    @Transactional(readOnly = true)
    @Operation(summary = "Attendance summary for a period (both dates inclusive)")
    Summary summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from)) {
            throw new InvalidInputException("to", "Must not be before 'from'");
        }
        var zone = clock.getZone();
        return new Summary(from, to,
                attendances.countByAttendedOnBetween(from, to),
                attendances.countDistinctBeneficiariesBetween(from, to),
                beneficiaries.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        from.atStartOfDay(zone).toInstant(), to.plusDays(1).atStartOfDay(zone).toInstant()),
                beneficiaries.countByActiveTrueAndFileExpiresOnBefore(LocalDate.now(clock)));
    }
}

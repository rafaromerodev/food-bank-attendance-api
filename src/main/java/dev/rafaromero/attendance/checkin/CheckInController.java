package dev.rafaromero.attendance.checkin;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import dev.rafaromero.attendance.beneficiary.BeneficiaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Check-ins")
class CheckInController {

    private final CheckInService service;
    private final Clock clock;

    CheckInController(CheckInService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    /** {@code confirmExpired} is optional and defaults to false. */
    record CardCheckInRequest(@NotBlank String cardUid, Boolean confirmExpired) {
    }

    record ManualCheckInRequest(@NotNull Long beneficiaryId) {
    }

    record AttendanceResponse(Long id, LocalDate attendedOn, Instant attendedAt, Attendance.Source source) {

        static AttendanceResponse of(Attendance a) {
            return a == null ? null : new AttendanceResponse(a.getId(), a.getAttendedOn(), a.getAttendedAt(), a.getSource());
        }
    }

    record CheckInResponse(CheckInOutcome outcome, BeneficiaryResponse beneficiary, AttendanceResponse attendance) {
    }

    @PostMapping("/api/check-ins")
    @Operation(summary = "Present a card at the reader",
            description = "Always answers 200 with an explicit outcome; only REGISTERED records an attendance.")
    CheckInResponse checkIn(@Valid @RequestBody CardCheckInRequest request) {
        return toResponse(service.checkIn(request.cardUid(), Boolean.TRUE.equals(request.confirmExpired())));
    }

    @PostMapping("/api/check-ins/manual")
    @Operation(summary = "Check in without a card")
    CheckInResponse checkInManually(@Valid @RequestBody ManualCheckInRequest request) {
        return toResponse(service.checkInManually(request.beneficiaryId()));
    }

    @GetMapping("/api/beneficiaries/{beneficiaryId}/attendances")
    @Operation(summary = "Attendance history, newest first")
    List<AttendanceResponse> history(@PathVariable long beneficiaryId) {
        return service.history(beneficiaryId).stream().map(AttendanceResponse::of).toList();
    }

    private CheckInResponse toResponse(CheckInService.Result result) {
        BeneficiaryResponse beneficiary = result.beneficiary() == null
                ? null : BeneficiaryResponse.of(result.beneficiary(), LocalDate.now(clock));
        return new CheckInResponse(result.outcome(), beneficiary, AttendanceResponse.of(result.attendance()));
    }
}

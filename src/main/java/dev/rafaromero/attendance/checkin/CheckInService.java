package dev.rafaromero.attendance.checkin;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import dev.rafaromero.attendance.beneficiary.Beneficiary;
import dev.rafaromero.attendance.beneficiary.BeneficiaryService;
import dev.rafaromero.attendance.beneficiary.FileStatus;
import dev.rafaromero.attendance.card.Card;
import dev.rafaromero.attendance.card.CardService;
import dev.rafaromero.attendance.checkin.Attendance.Source;
import dev.rafaromero.attendance.common.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decides what happens when a card is presented at the reader.
 *
 * <p>All the decision logic lives here, independent of the HTTP layer, so every case can be
 * tested without hardware. The unique constraint on (beneficiary, day) in the database is the
 * final guarantee against double counting if two requests race.
 */
@Service
@Transactional
public class CheckInService {

    private final CardService cards;
    private final BeneficiaryService beneficiaries;
    private final AttendanceRepository attendances;
    private final Clock clock;

    CheckInService(CardService cards, BeneficiaryService beneficiaries, AttendanceRepository attendances,
            Clock clock) {
        this.cards = cards;
        this.beneficiaries = beneficiaries;
        this.attendances = attendances;
        this.clock = clock;
    }

    public record Result(CheckInOutcome outcome, Beneficiary beneficiary, Attendance attendance) {
    }

    /**
     * @param confirmExpired staff confirmed the visit even though the file has expired
     */
    public Result checkIn(String rawUid, boolean confirmExpired) {
        String uid = CardService.normalizeUid(rawUid);
        Optional<Card> active = cards.findActive(uid);
        if (active.isEmpty()) {
            // Load the owner inside the transaction: the card only holds a lazy reference.
            return cards.findLastDeactivated(uid)
                    .map(old -> new Result(CheckInOutcome.DEACTIVATED_CARD,
                            beneficiaries.get(old.getBeneficiary().getId()), null))
                    .orElseGet(() -> new Result(CheckInOutcome.UNKNOWN_CARD, null, null));
        }
        Beneficiary beneficiary = active.get().getBeneficiary();
        if (!beneficiary.isActive()) {
            return new Result(CheckInOutcome.INACTIVE_BENEFICIARY, beneficiary, null);
        }
        LocalDate today = LocalDate.now(clock);
        Optional<Attendance> existing = attendances.findByBeneficiaryIdAndAttendedOn(beneficiary.getId(), today);
        if (existing.isPresent()) {
            return new Result(CheckInOutcome.ALREADY_ATTENDED_TODAY, beneficiary, existing.get());
        }
        if (beneficiary.fileStatus(today) == FileStatus.EXPIRED && !confirmExpired) {
            return new Result(CheckInOutcome.FILE_EXPIRED, beneficiary, null);
        }
        return register(beneficiary, today, Source.CARD);
    }

    /** Check-in without a card (forgotten card, reader out of order). */
    public Result checkInManually(long beneficiaryId) {
        Beneficiary beneficiary = beneficiaries.get(beneficiaryId);
        if (!beneficiary.isActive()) {
            throw new ConflictException("Beneficiary " + beneficiaryId + " is inactive");
        }
        LocalDate today = LocalDate.now(clock);
        return attendances.findByBeneficiaryIdAndAttendedOn(beneficiaryId, today)
                .map(existing -> new Result(CheckInOutcome.ALREADY_ATTENDED_TODAY, beneficiary, existing))
                .orElseGet(() -> register(beneficiary, today, Source.MANUAL));
    }

    @Transactional(readOnly = true)
    public List<Attendance> history(long beneficiaryId) {
        beneficiaries.get(beneficiaryId);
        return attendances.findByBeneficiaryIdOrderByAttendedAtDescIdDesc(beneficiaryId);
    }

    private Result register(Beneficiary beneficiary, LocalDate today, Source source) {
        Attendance attendance = attendances.save(new Attendance(beneficiary, today, Instant.now(clock), source));
        return new Result(CheckInOutcome.REGISTERED, beneficiary, attendance);
    }
}

package dev.rafaromero.attendance.card;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import dev.rafaromero.attendance.beneficiary.Beneficiary;
import dev.rafaromero.attendance.beneficiary.BeneficiaryService;
import dev.rafaromero.attendance.common.ConflictException;
import dev.rafaromero.attendance.common.InvalidInputException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardService {

    private static final Pattern HEX_UID = Pattern.compile("[0-9A-F]{8,20}");

    private final CardRepository cards;
    private final BeneficiaryService beneficiaries;
    private final Clock clock;

    CardService(CardRepository cards, BeneficiaryService beneficiaries, Clock clock) {
        this.cards = cards;
        this.beneficiaries = beneficiaries;
        this.clock = clock;
    }

    /** Readers report UIDs in different shapes ("04 a1:b2-c3"); they are stored as plain upper-case hex. */
    public static String normalizeUid(String raw) {
        String uid = raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[\\s:\\-]", "");
        if (!HEX_UID.matcher(uid).matches()) {
            throw new InvalidInputException("uid", "Card UID must be 8 to 20 hexadecimal characters");
        }
        return uid;
    }

    /** First card for a beneficiary. Replacing an existing card goes through {@link #replace}. */
    public Card assign(long beneficiaryId, String rawUid) {
        Beneficiary beneficiary = requireActive(beneficiaryId);
        if (cards.findByBeneficiaryIdAndDeactivatedAtIsNull(beneficiaryId).isPresent()) {
            throw new ConflictException("Beneficiary " + beneficiaryId + " already has an active card; replace it instead");
        }
        return cards.save(new Card(requireFreeUid(rawUid), beneficiary, Instant.now(clock)));
    }

    /** Deactivates the current card with a reason and assigns a new one. The attendance history is untouched. */
    public Card replace(long beneficiaryId, String rawUid, DeactivationReason reason) {
        Beneficiary beneficiary = requireActive(beneficiaryId);
        String uid = requireFreeUid(rawUid);
        Instant now = Instant.now(clock);
        Card current = cards.findByBeneficiaryIdAndDeactivatedAtIsNull(beneficiaryId)
                .orElseThrow(() -> new ConflictException("Beneficiary " + beneficiaryId + " has no active card to replace"));
        current.deactivate(reason, now);
        cards.flush(); // free the "one active card per person" slot before inserting the new card
        return cards.save(new Card(uid, beneficiary, now));
    }

    @Transactional(readOnly = true)
    public List<Card> history(long beneficiaryId) {
        beneficiaries.get(beneficiaryId);
        return cards.findByBeneficiaryIdOrderByAssignedAtDescIdDesc(beneficiaryId);
    }

    @Transactional(readOnly = true)
    public Optional<Card> findActive(String uid) {
        return cards.findByUidAndDeactivatedAtIsNull(uid);
    }

    @Transactional(readOnly = true)
    public Optional<Card> findLastDeactivated(String uid) {
        return cards.findFirstByUidAndDeactivatedAtIsNotNullOrderByDeactivatedAtDesc(uid);
    }

    private Beneficiary requireActive(long beneficiaryId) {
        Beneficiary beneficiary = beneficiaries.get(beneficiaryId);
        if (!beneficiary.isActive()) {
            throw new ConflictException("Beneficiary " + beneficiaryId + " is inactive");
        }
        return beneficiary;
    }

    private String requireFreeUid(String rawUid) {
        String uid = normalizeUid(rawUid);
        cards.findByUidAndDeactivatedAtIsNull(uid).ifPresent(card -> {
            throw new ConflictException("Card " + uid + " is already assigned to beneficiary "
                    + card.getBeneficiary().getId());
        });
        return uid;
    }
}

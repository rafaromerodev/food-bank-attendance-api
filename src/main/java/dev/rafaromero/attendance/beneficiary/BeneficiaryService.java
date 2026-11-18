package dev.rafaromero.attendance.beneficiary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

import dev.rafaromero.attendance.common.ConflictException;
import dev.rafaromero.attendance.common.IdentityDocuments;
import dev.rafaromero.attendance.common.InvalidInputException;
import dev.rafaromero.attendance.common.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BeneficiaryService {

    private final BeneficiaryRepository beneficiaries;
    private final Clock clock;

    BeneficiaryService(BeneficiaryRepository beneficiaries, Clock clock) {
        this.beneficiaries = beneficiaries;
        this.clock = clock;
    }

    public Beneficiary create(BeneficiaryRequest request) {
        String document = IdentityDocuments.requireValid(request.document());
        if (beneficiaries.existsByDocument(document)) {
            throw new ConflictException("A beneficiary with document " + document + " already exists");
        }
        return beneficiaries.save(new Beneficiary(document, request, Instant.now(clock)));
    }

    /** Updates the record, including the document: the internal id is the key, so a typo can be fixed safely. */
    public Beneficiary update(long id, BeneficiaryRequest request) {
        Beneficiary beneficiary = get(id);
        String document = IdentityDocuments.requireValid(request.document());
        if (!document.equals(beneficiary.getDocument()) && beneficiaries.existsByDocument(document)) {
            throw new ConflictException("A beneficiary with document " + document + " already exists");
        }
        Instant now = Instant.now(clock);
        beneficiary.correctDocument(document, now);
        beneficiary.apply(request, now);
        return beneficiary;
    }

    @Transactional(readOnly = true)
    public Beneficiary get(long id) {
        return beneficiaries.findById(id).orElseThrow(() -> new NotFoundException("Beneficiary", id));
    }

    @Transactional(readOnly = true)
    public Page<Beneficiary> search(String query, boolean active, Pageable pageable) {
        String normalized = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return beneficiaries.search(normalized, active, pageable);
    }

    public Beneficiary renew(long id, int months) {
        if (months != 6 && months != 12) {
            throw new InvalidInputException("months", "Files are renewed for 6 or 12 months");
        }
        Beneficiary beneficiary = get(id);
        beneficiary.renew(months, today(), Instant.now(clock));
        return beneficiary;
    }

    /** Soft delete: the attendance history is kept so past reports stay the same. */
    public void deactivate(long id) {
        get(id).deactivate(Instant.now(clock));
    }

    public Beneficiary reactivate(long id) {
        Beneficiary beneficiary = get(id);
        beneficiary.reactivate(Instant.now(clock));
        return beneficiary;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }
}

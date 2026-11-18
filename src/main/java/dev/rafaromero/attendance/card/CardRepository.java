package dev.rafaromero.attendance.card;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, Long> {

    Optional<Card> findByUidAndDeactivatedAtIsNull(String uid);

    Optional<Card> findFirstByUidAndDeactivatedAtIsNotNullOrderByDeactivatedAtDesc(String uid);

    Optional<Card> findByBeneficiaryIdAndDeactivatedAtIsNull(Long beneficiaryId);

    List<Card> findByBeneficiaryIdOrderByAssignedAtDescIdDesc(Long beneficiaryId);
}

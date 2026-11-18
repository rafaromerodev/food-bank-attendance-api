package dev.rafaromero.attendance.beneficiary;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    boolean existsByDocument(String document);

    /** Matches name, document or file number. An empty query returns everyone with the given status. */
    @Query("""
            select b from Beneficiary b
            where b.active = :active
              and (lower(concat(b.firstName, ' ', b.lastName)) like concat('%', :query, '%')
                   or b.document like concat('%', upper(:query), '%')
                   or lower(coalesce(b.fileNumber, '')) like concat('%', :query, '%'))
            """)
    Page<Beneficiary> search(@Param("query") String query, @Param("active") boolean active, Pageable pageable);

    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant from, Instant to);

    long countByActiveTrueAndFileExpiresOnBefore(LocalDate date);
}

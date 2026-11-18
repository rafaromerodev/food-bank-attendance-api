package dev.rafaromero.attendance.card;

import java.time.Instant;

import dev.rafaromero.attendance.beneficiary.Beneficiary;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** An NFC card assigned to a beneficiary. Deactivated cards are kept as history. */
@Entity
@Table(name = "card")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id")
    private Beneficiary beneficiary;

    @Column(nullable = false)
    private Instant assignedAt;

    private Instant deactivatedAt;

    @Enumerated(EnumType.STRING)
    private DeactivationReason deactivationReason;

    protected Card() {
        // for JPA
    }

    Card(String uid, Beneficiary beneficiary, Instant now) {
        this.uid = uid;
        this.beneficiary = beneficiary;
        this.assignedAt = now;
    }

    void deactivate(DeactivationReason reason, Instant now) {
        this.deactivatedAt = now;
        this.deactivationReason = reason;
    }

    public boolean isActive() {
        return deactivatedAt == null;
    }

    public Long getId() {
        return id;
    }

    public String getUid() {
        return uid;
    }

    public Beneficiary getBeneficiary() {
        return beneficiary;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public DeactivationReason getDeactivationReason() {
        return deactivationReason;
    }
}

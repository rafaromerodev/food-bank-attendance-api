package dev.rafaromero.attendance.checkin;

import java.time.Instant;
import java.time.LocalDate;

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

/** One visit to the food bank. Belongs to the beneficiary, never to the card. */
@Entity
@Table(name = "attendance")
public class Attendance {

    public enum Source { CARD, MANUAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "beneficiary_id")
    private Beneficiary beneficiary;

    @Column(nullable = false)
    private LocalDate attendedOn;

    @Column(nullable = false)
    private Instant attendedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Source source;

    protected Attendance() {
        // for JPA
    }

    Attendance(Beneficiary beneficiary, LocalDate attendedOn, Instant attendedAt, Source source) {
        this.beneficiary = beneficiary;
        this.attendedOn = attendedOn;
        this.attendedAt = attendedAt;
        this.source = source;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getAttendedOn() {
        return attendedOn;
    }

    public Instant getAttendedAt() {
        return attendedAt;
    }

    public Source getSource() {
        return source;
    }
}

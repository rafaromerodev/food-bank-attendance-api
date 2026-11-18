package dev.rafaromero.attendance.beneficiary;

import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "beneficiary")
public class Beneficiary {

    static final int EXPIRY_WARNING_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String document;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String phone;

    private String fileNumber;
    private Integer householdAdults;
    private Integer householdMinors;
    private LocalDate fileExpiresOn;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Beneficiary() {
        // for JPA
    }

    Beneficiary(String document, BeneficiaryRequest details, Instant now) {
        this.document = document;
        this.createdAt = now;
        apply(details, now);
    }

    void apply(BeneficiaryRequest details, Instant now) {
        this.firstName = details.firstName().strip();
        this.lastName = details.lastName().strip();
        this.phone = details.phone().strip();
        this.fileNumber = details.fileNumber();
        this.householdAdults = details.householdAdults();
        this.householdMinors = details.householdMinors();
        this.fileExpiresOn = details.fileExpiresOn();
        this.updatedAt = now;
    }

    void correctDocument(String document, Instant now) {
        this.document = document;
        this.updatedAt = now;
    }

    /** Extends the file from today or from its current expiry, whichever is later. */
    void renew(int months, LocalDate today, Instant now) {
        LocalDate from = fileExpiresOn == null || fileExpiresOn.isBefore(today) ? today : fileExpiresOn;
        this.fileExpiresOn = from.plusMonths(months);
        this.updatedAt = now;
    }

    void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    void reactivate(Instant now) {
        this.active = true;
        this.updatedAt = now;
    }

    public FileStatus fileStatus(LocalDate today) {
        if (fileExpiresOn == null) {
            return FileStatus.NOT_SET;
        }
        if (fileExpiresOn.isBefore(today)) {
            return FileStatus.EXPIRED;
        }
        return fileExpiresOn.isAfter(today.plusDays(EXPIRY_WARNING_DAYS)) ? FileStatus.VALID : FileStatus.EXPIRING_SOON;
    }

    public Long getId() {
        return id;
    }

    public String getDocument() {
        return document;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhone() {
        return phone;
    }

    public String getFileNumber() {
        return fileNumber;
    }

    public Integer getHouseholdAdults() {
        return householdAdults;
    }

    public Integer getHouseholdMinors() {
        return householdMinors;
    }

    public LocalDate getFileExpiresOn() {
        return fileExpiresOn;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

package dev.rafaromero.attendance.beneficiary;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Body for creating or updating a beneficiary. The document's check letter is verified by the service. */
public record BeneficiaryRequest(
        @NotBlank String document,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 120) String lastName,
        @NotBlank @Pattern(regexp = "\\+?[0-9 ]{9,15}", message = "must be a phone number") String phone,
        @Size(max = 30) String fileNumber,
        @PositiveOrZero Integer householdAdults,
        @PositiveOrZero Integer householdMinors,
        LocalDate fileExpiresOn) {
}

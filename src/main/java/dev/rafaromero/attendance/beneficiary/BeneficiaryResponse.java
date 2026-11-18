package dev.rafaromero.attendance.beneficiary;

import java.time.LocalDate;

public record BeneficiaryResponse(
        Long id,
        String document,
        String firstName,
        String lastName,
        String phone,
        String fileNumber,
        Integer householdAdults,
        Integer householdMinors,
        LocalDate fileExpiresOn,
        FileStatus fileStatus,
        boolean active) {

    public static BeneficiaryResponse of(Beneficiary b, LocalDate today) {
        return new BeneficiaryResponse(b.getId(), b.getDocument(), b.getFirstName(), b.getLastName(),
                b.getPhone(), b.getFileNumber(), b.getHouseholdAdults(), b.getHouseholdMinors(),
                b.getFileExpiresOn(), b.fileStatus(today), b.isActive());
    }
}

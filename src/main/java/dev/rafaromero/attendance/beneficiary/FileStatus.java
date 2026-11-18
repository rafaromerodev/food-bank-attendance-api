package dev.rafaromero.attendance.beneficiary;

/** State of a beneficiary's social-services file on a given day. */
public enum FileStatus {
    VALID,
    /** Valid, but expires within {@link Beneficiary#EXPIRY_WARNING_DAYS} days. */
    EXPIRING_SOON,
    EXPIRED,
    /** No expiry date recorded yet. */
    NOT_SET
}

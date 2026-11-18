package dev.rafaromero.attendance.checkin;

/** What happened when a card was presented. The client shows a different screen for each case. */
public enum CheckInOutcome {
    /** A new attendance was recorded. */
    REGISTERED,
    /** The person was already attended today; nothing new was recorded. */
    ALREADY_ATTENDED_TODAY,
    /** The file has expired. Nothing is recorded until staff renew it or confirm the visit. */
    FILE_EXPIRED,
    /** The card is not assigned to anyone: the client should start a registration. */
    UNKNOWN_CARD,
    /** The card was replaced (lost, stolen...) and is no longer valid. */
    DEACTIVATED_CARD,
    /** The card belongs to a deactivated beneficiary. */
    INACTIVE_BENEFICIARY
}

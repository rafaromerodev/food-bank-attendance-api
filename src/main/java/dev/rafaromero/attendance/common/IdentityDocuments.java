package dev.rafaromero.attendance.common;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Normalisation and validation of Spanish identity documents (DNI and NIE).
 *
 * <p>The last letter is a checksum of the number, so a typo can be detected and the
 * correct letter suggested instead of silently storing a wrong document.
 */
public final class IdentityDocuments {

    private static final String CHECK_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final Pattern DNI = Pattern.compile("\\d{8}[A-Z]");
    private static final Pattern NIE = Pattern.compile("[XYZ]\\d{7}[A-Z]");

    private IdentityDocuments() {
    }

    /** Upper case, without spaces, dots or dashes: {@code "12.345.678-z"} becomes {@code "12345678Z"}. */
    public static String normalize(String raw) {
        return raw == null ? "" : raw.toUpperCase(Locale.ROOT).replaceAll("[\\s.\\-]", "");
    }

    /**
     * Returns the document with its correct check letter if the format is valid.
     * The result equals the input when the letter is already right.
     */
    public static Optional<String> withCorrectLetter(String normalized) {
        String digits;
        if (DNI.matcher(normalized).matches()) {
            digits = normalized.substring(0, 8);
        } else if (NIE.matcher(normalized).matches()) {
            digits = "XYZ".indexOf(normalized.charAt(0)) + normalized.substring(1, 8);
        } else {
            return Optional.empty();
        }
        char expected = CHECK_LETTERS.charAt(Integer.parseInt(digits) % 23);
        return Optional.of(normalized.substring(0, normalized.length() - 1) + expected);
    }

    /** Normalises and validates, throwing a user-facing error with the suggested correction. */
    public static String requireValid(String raw) {
        String normalized = normalize(raw);
        String corrected = withCorrectLetter(normalized)
                .orElseThrow(() -> new InvalidInputException("document", "Must be a DNI (12345678Z) or NIE (X1234567L)"));
        if (!corrected.equals(normalized)) {
            throw new InvalidInputException("document", "Wrong check letter. Did you mean " + corrected + "?");
        }
        return normalized;
    }
}

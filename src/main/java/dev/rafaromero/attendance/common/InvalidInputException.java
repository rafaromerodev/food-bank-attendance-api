package dev.rafaromero.attendance.common;

/** A business-rule validation error on a single field, reported as 400 with the field name. */
public class InvalidInputException extends RuntimeException {

    private final String field;

    public InvalidInputException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

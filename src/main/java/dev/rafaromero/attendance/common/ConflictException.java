package dev.rafaromero.attendance.common;

/** A request that is valid on its own but clashes with the current state (duplicate document, card in use...). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

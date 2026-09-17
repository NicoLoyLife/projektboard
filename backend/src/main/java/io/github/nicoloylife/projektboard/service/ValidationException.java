package io.github.nicoloylife.projektboard.service;

/** Fachlicher Validierungsfehler mit Bezug auf ein Eingabefeld, wird als 400 mit Feldfehler ausgeliefert. */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}

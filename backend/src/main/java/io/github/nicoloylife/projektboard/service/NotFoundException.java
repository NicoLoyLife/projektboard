package io.github.nicoloylife.projektboard.service;

/** Das Objekt existiert nicht oder ist für den Benutzer nicht sichtbar. */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}

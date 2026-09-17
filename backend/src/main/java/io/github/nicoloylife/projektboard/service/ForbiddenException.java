package io.github.nicoloylife.projektboard.service;

/** Der Benutzer ist angemeldet, darf diese Aktion aber nicht ausführen. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}

package io.github.nicoloylife.projektboard.service;

/** Die Aktion widerspricht dem Zustand des Objekts, etwa bei archivierten Projekten. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}

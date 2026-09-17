package io.github.nicoloylife.projektboard.api;

import io.github.nicoloylife.projektboard.api.dto.ErrorResponse;
import io.github.nicoloylife.projektboard.service.ConflictException;
import io.github.nicoloylife.projektboard.service.ForbiddenException;
import io.github.nicoloylife.projektboard.service.NotFoundException;
import io.github.nicoloylife.projektboard.service.ValidationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Übersetzt Ausnahmen zentral in die Fehlerstruktur der API. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorResponse.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ErrorResponse.validation(errors);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ValidationException.class)
    public ErrorResponse handleBusinessValidation(ValidationException exception) {
        return ErrorResponse.validation(List.of(new ErrorResponse.FieldError(exception.getField(), exception.getMessage())));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ErrorResponse handleUnreadable(HttpMessageNotReadableException exception) {
        return ErrorResponse.of("bad_request", "Die Anfrage konnte nicht gelesen werden");
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ErrorResponse handleAuthentication(AuthenticationException exception) {
        return ErrorResponse.of("unauthorized", "Benutzername oder Passwort ist falsch");
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
    public ErrorResponse handleForbidden(RuntimeException exception) {
        String message = exception instanceof ForbiddenException
                ? exception.getMessage()
                : "Keine Berechtigung für diese Aktion";
        return ErrorResponse.of("forbidden", message);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ErrorResponse handleNotFound(NotFoundException exception) {
        return ErrorResponse.of("not_found", exception.getMessage());
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConflictException.class)
    public ErrorResponse handleConflict(ConflictException exception) {
        return ErrorResponse.of("conflict", exception.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponse handleGeneric(Exception exception) {
        log.error("Unerwarteter Fehler bei der Verarbeitung einer Anfrage", exception);
        return ErrorResponse.of("internal_error", "Ein unerwarteter Fehler ist aufgetreten");
    }
}

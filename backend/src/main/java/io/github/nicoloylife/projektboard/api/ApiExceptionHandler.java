package io.github.nicoloylife.projektboard.api;

import io.github.nicoloylife.projektboard.api.dto.ErrorResponse;
import io.github.nicoloylife.projektboard.security.CurrentUserService;
import io.github.nicoloylife.projektboard.service.ConflictException;
import io.github.nicoloylife.projektboard.service.ForbiddenException;
import io.github.nicoloylife.projektboard.service.NotFoundException;
import io.github.nicoloylife.projektboard.service.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Übersetzt Ausnahmen zentral in die Fehlerstruktur der API. */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private static final String LOGIN_REQUIRED = "Anmeldung erforderlich";

    private final CurrentUserService currentUserService;

    public ApiExceptionHandler(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

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

    /** Eine Pfadvariable oder ein Parameter hat den falschen Typ, etwa /api/projects/abc. */
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TypeMismatchException.class)
    public ErrorResponse handleTypeMismatch(TypeMismatchException exception) {
        return ErrorResponse.of("bad_request", "Die Anfrage ist ungültig");
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationException.class)
    public ErrorResponse handleAuthentication(AuthenticationException exception) {
        return ErrorResponse.of("unauthorized", "Benutzername oder Passwort ist falsch");
    }

    /**
     * Die Sitzung fehlt oder gilt nicht mehr, etwa nach einer Deaktivierung. Sie wird beendet, damit sie nach einer
     * Reaktivierung nicht wieder auflebt. Eine in der Zwischenzeit unbenutzte Sitzung endet erst durch Zeitablauf.
     * Die Antwort gleicht der ohne Sitzung aus der Sicherheitskonfiguration.
     */
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ErrorResponse handleMissingAuthentication(AuthenticationCredentialsNotFoundException exception,
            HttpServletRequest request) {
        return endSession(request);
    }

    /**
     * Ein 403 aus der Methodensicherheit kann auch von einer Sitzung stammen, deren Rolle sich inzwischen geändert
     * hat. Dann gilt die Sitzung nicht mehr und die Antwort ist wie bei fehlender Anmeldung ein 401.
     */
    @ExceptionHandler({AccessDeniedException.class, ForbiddenException.class})
    public ResponseEntity<ErrorResponse> handleForbidden(RuntimeException exception, HttpServletRequest request) {
        if (exception instanceof AccessDeniedException && sessionIsStale()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(endSession(request));
        }
        String message = exception instanceof ForbiddenException
                ? exception.getMessage()
                : "Keine Berechtigung für diese Aktion";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of("forbidden", message));
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

    /**
     * Fehler, die Spring selbst mit einem Statuscode versieht (unbekannte Adresse, nicht erlaubte Methode,
     * falscher Inhaltstyp, fehlender Parameter), bleiben Fehler des Aufrufers. Alles andere ist unerwartet,
     * ergibt 500 und landet mit Einzelheiten nur im Protokoll.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception exception) {
        if (exception instanceof org.springframework.web.ErrorResponse springError) {
            HttpStatus status = HttpStatus.valueOf(springError.getStatusCode().value());
            log.debug("Anfrage abgewiesen mit {}: {}", status.value(), exception.getMessage());
            return ResponseEntity.status(status)
                    .body(ErrorResponse.of(status.name().toLowerCase(), clientErrorMessage(status)));
        }
        log.error("Unerwarteter Fehler bei der Verarbeitung einer Anfrage", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("internal_error", "Ein unerwarteter Fehler ist aufgetreten"));
    }

    private boolean sessionIsStale() {
        try {
            currentUserService.require();
            return false;
        } catch (AuthenticationCredentialsNotFoundException stale) {
            return true;
        }
    }

    private static ErrorResponse endSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ErrorResponse.of("unauthorized", LOGIN_REQUIRED);
    }

    private static String clientErrorMessage(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Die Anfrage ist ungültig";
            case NOT_FOUND -> "Die Adresse ist unbekannt";
            case METHOD_NOT_ALLOWED -> "Die Methode ist für diese Adresse nicht erlaubt";
            case UNSUPPORTED_MEDIA_TYPE -> "Der Inhaltstyp wird nicht unterstützt";
            default -> "Die Anfrage kann nicht verarbeitet werden";
        };
    }
}

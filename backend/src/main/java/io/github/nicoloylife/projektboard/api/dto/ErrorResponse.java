package io.github.nicoloylife.projektboard.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Einheitliche Fehlerantwort der API. Feldfehler gibt es nur bei Validierungsfehlern. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(String error, String message, List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(String error, String message) {
        return new ErrorResponse(error, message, null);
    }

    public static ErrorResponse validation(List<FieldError> errors) {
        return new ErrorResponse("validation_failed", "Eingaben sind ungültig", errors);
    }
}

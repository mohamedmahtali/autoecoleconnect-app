package app.autoeecoleconnect.controlplane.exceptions;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduit les exceptions en réponses RFC 9457 (Problem Details).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailGerantDejaUtiliseException.class)
    public ProblemDetail emailGerantDejaUtilise(EmailGerantDejaUtiliseException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(InviteTokenInvalideException.class)
    public ProblemDetail inviteTokenInvalide(InviteTokenInvalideException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ProvisioningException.class)
    public ProblemDetail provisioningEchoue(ProvisioningException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                "Le provisioning n'a pas pu démarrer, réessayez plus tard");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validationInvalide(MethodArgumentNotValidException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Requête invalide");
        Map<String, String> erreurs = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(err -> erreurs.put(err.getField(), err.getDefaultMessage()));
        detail.setProperty("erreurs", erreurs);
        return detail;
    }
}

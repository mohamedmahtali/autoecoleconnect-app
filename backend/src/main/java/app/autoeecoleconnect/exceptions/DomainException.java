package app.autoeecoleconnect.exceptions;

/**
 * Base des exceptions métier : levées par les services, traduites en réponse
 * HTTP par le {@link GlobalExceptionHandler}.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

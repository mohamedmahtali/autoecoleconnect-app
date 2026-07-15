package app.autoeecoleconnect.exceptions;

public class EmailDejaUtiliseException extends DomainException {

    public EmailDejaUtiliseException(String email) {
        super("L'email %s est déjà utilisé".formatted(email));
    }
}

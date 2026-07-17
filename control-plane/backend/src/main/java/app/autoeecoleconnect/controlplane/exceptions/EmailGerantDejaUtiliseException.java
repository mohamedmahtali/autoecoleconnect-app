package app.autoeecoleconnect.controlplane.exceptions;

public class EmailGerantDejaUtiliseException extends DomainException {

    public EmailGerantDejaUtiliseException(String email) {
        super("Une organisation existe déjà avec l'email gérant : " + email);
    }
}

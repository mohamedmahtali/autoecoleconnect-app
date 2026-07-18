package app.autoeecoleconnect.controlplane.exceptions;

public class IdentifiantsInvalidesException extends DomainException {

    public IdentifiantsInvalidesException() {
        super("Email ou mot de passe invalide");
    }
}

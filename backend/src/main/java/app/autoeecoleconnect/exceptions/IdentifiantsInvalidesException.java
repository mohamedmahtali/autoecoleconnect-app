package app.autoeecoleconnect.exceptions;

public class IdentifiantsInvalidesException extends DomainException {

    // Message volontairement générique : ne pas révéler si l'email existe.
    public IdentifiantsInvalidesException() {
        super("Email ou mot de passe incorrect");
    }
}

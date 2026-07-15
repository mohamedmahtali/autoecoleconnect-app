package app.autoeecoleconnect.exceptions;

public class RessourceIntrouvableException extends DomainException {

    public RessourceIntrouvableException(String ressource, Object id) {
        super("%s %s introuvable".formatted(ressource, id));
    }
}

package app.autoeecoleconnect.exceptions;

public class RessourceIntrouvableException extends DomainException {

    public RessourceIntrouvableException(String ressource, Long id) {
        super("%s %d introuvable".formatted(ressource, id));
    }
}

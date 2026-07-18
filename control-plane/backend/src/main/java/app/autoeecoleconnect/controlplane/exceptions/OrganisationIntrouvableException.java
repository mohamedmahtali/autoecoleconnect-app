package app.autoeecoleconnect.controlplane.exceptions;

public class OrganisationIntrouvableException extends DomainException {

    public OrganisationIntrouvableException() {
        super("Organisation introuvable");
    }
}

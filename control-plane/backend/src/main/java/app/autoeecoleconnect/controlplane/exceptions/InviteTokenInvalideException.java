package app.autoeecoleconnect.controlplane.exceptions;

public class InviteTokenInvalideException extends DomainException {

    public InviteTokenInvalideException() {
        super("Header X-Invite-Token absent ou invalide");
    }
}

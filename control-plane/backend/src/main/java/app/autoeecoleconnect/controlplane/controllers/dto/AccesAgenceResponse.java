package app.autoeecoleconnect.controlplane.controllers.dto;

/**
 * Ce que le gérant reçoit pour entrer dans une de ses agences sans se
 * réauthentifier : un jeton émis par le tenant lui-même, et l'URL où
 * l'utiliser (docs/18 §18.3 lot 5).
 */
public record AccesAgenceResponse(String token, String type, String url) {
}

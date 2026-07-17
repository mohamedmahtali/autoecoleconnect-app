package app.autoeecoleconnect.controlplane.services;

/**
 * Envoi d'emails transactionnels (Resend) — voir docs/09 §9.2 étape 7.
 */
public interface EmailService {

    void envoyerBienvenue(String destinataire, String nomAutoEcole, String url,
                           String adminEmail, String adminPassword);
}

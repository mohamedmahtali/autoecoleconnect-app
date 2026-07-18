package app.autoeecoleconnect.controlplane.services;

/**
 * Envoi d'emails transactionnels (Resend) — voir docs/09 §9.2 étape 7 (bienvenue)
 * et §9.3-9.4 (rappel / fin d'essai, Slice B).
 */
public interface EmailService {

    void envoyerBienvenue(String destinataire, String nomAutoEcole, String url,
                           String adminEmail, String adminPassword);

    void envoyerRappelEssai(String destinataire, String nomOrganisation, long joursRestants);

    void envoyerFinEssai(String destinataire, String nomOrganisation);

    void envoyerConfirmationAbonnement(String destinataire, String nomOrganisation, String plan);

    void envoyerEchecPaiement(String destinataire, String nomOrganisation);

    void envoyerConfirmationSuppression(String destinataire, String nomOrganisation);
}

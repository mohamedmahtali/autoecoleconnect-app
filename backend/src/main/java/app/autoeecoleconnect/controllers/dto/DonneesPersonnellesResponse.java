package app.autoeecoleconnect.controllers.dto;

import java.util.List;

import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.Examen;
import app.autoeecoleconnect.models.Reservation;
import app.autoeecoleconnect.models.Seance;

// Export RGPD « droit d'accès » (docs/12 §12.6) : toutes les données
// personnelles détenues sur un élève, qu'il peut télécharger lui-même.
// Réutilise les DTO de réponse existants — donc jamais le passwordHash, déjà
// exclu de ClientResponse.
public record DonneesPersonnellesResponse(
        ClientResponse identite,
        List<ReservationResponse> reservations,
        List<SeanceResponse> seances,
        List<ExamenResponse> examens) {

    public static DonneesPersonnellesResponse depuis(
            Client client, List<Reservation> reservations, List<Seance> seances,
            List<Examen> examens) {
        return new DonneesPersonnellesResponse(
                ClientResponse.depuis(client),
                reservations.stream().map(ReservationResponse::depuis).toList(),
                seances.stream().map(SeanceResponse::depuis).toList(),
                examens.stream().map(ExamenResponse::depuis).toList());
    }
}

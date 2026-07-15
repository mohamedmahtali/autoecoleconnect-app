package app.autoeecoleconnect.controllers.dto;

import java.time.Instant;
import java.time.LocalDate;

import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.StatutClient;

public record ClientResponse(
        Long id,
        String nom,
        String prenom,
        String email,
        String telephone,
        LocalDate dateNaissance,
        StatutClient statut,
        Instant creeLe,
        Instant modifieLe) {

    public static ClientResponse depuis(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getNom(),
                client.getPrenom(),
                client.getEmail(),
                client.getTelephone(),
                client.getDateNaissance(),
                client.getStatut(),
                client.getCreeLe(),
                client.getModifieLe());
    }
}

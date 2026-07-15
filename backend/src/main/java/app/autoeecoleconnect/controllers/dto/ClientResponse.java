package app.autoeecoleconnect.controllers.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import app.autoeecoleconnect.models.Client;

// Ne jamais exposer passwordHash dans une réponse API.
public record ClientResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        String notes,
        boolean active,
        LocalDateTime createdAt) {

    public static ClientResponse depuis(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getNom(),
                client.getPrenom(),
                client.getEmail(),
                client.getTelephone(),
                client.getAdresse(),
                client.getNotes(),
                client.isActive(),
                client.getCreatedAt());
    }
}

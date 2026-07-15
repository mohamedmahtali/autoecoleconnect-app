package app.autoeecoleconnect.services;

import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import app.autoeecoleconnect.exceptions.CompteNonApprouveException;
import app.autoeecoleconnect.exceptions.IdentifiantsInvalidesException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.Directeur;
import app.autoeecoleconnect.models.Moniteur;
import app.autoeecoleconnect.models.StatutMoniteur;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.DirecteurRepository;
import app.autoeecoleconnect.repositories.MoniteurRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Un seul endpoint de login pour les trois profils : le rôle embarqué dans le
 * JWT (DIRECTEUR, MONITEUR, CLIENT) détermine ensuite les autorisations.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final DirecteurRepository directeurRepository;
    private final MoniteurRepository moniteurRepository;
    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(DirecteurRepository directeurRepository,
                       MoniteurRepository moniteurRepository,
                       ClientRepository clientRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.directeurRepository = directeurRepository;
        this.moniteurRepository = moniteurRepository;
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
        var directeur = directeurRepository.findByEmailAndActiveTrue(request.email());
        if (directeur.isPresent()) {
            return loginDirecteur(directeur.get(), request.motDePasse());
        }
        var moniteur = moniteurRepository.findByEmailAndActiveTrue(request.email());
        if (moniteur.isPresent()) {
            return loginMoniteur(moniteur.get(), request.motDePasse());
        }
        var client = clientRepository.findByEmailAndActiveTrue(request.email());
        if (client.isPresent()) {
            return loginClient(client.get(), request.motDePasse());
        }
        throw new IdentifiantsInvalidesException();
    }

    private LoginResponse loginDirecteur(Directeur directeur, String motDePasse) {
        verifierMotDePasse(motDePasse, directeur.getPasswordHash());
        return emettre(directeur.getId(), directeur.getEmail(), "DIRECTEUR",
                directeur.getPrenom() + " " + directeur.getNom());
    }

    private LoginResponse loginMoniteur(Moniteur moniteur, String motDePasse) {
        verifierMotDePasse(motDePasse, moniteur.getPasswordHash());
        // Le mot de passe est vérifié d'abord : le statut du compte n'est
        // révélé qu'à son propriétaire légitime.
        if (moniteur.getStatut() != StatutMoniteur.APPROVED) {
            throw new CompteNonApprouveException(
                    "Compte moniteur non approuvé (statut : %s)"
                            .formatted(moniteur.getStatut()));
        }
        return emettre(moniteur.getId(), moniteur.getEmail(), "MONITEUR",
                moniteur.getPrenom() + " " + moniteur.getNom());
    }

    private LoginResponse loginClient(Client client, String motDePasse) {
        verifierMotDePasse(motDePasse, client.getPasswordHash());
        return emettre(client.getId(), client.getEmail(), "CLIENT",
                client.getPrenom() + " " + client.getNom());
    }

    private void verifierMotDePasse(String motDePasse, String hash) {
        if (!passwordEncoder.matches(motDePasse, hash)) {
            throw new IdentifiantsInvalidesException();
        }
    }

    private LoginResponse emettre(UUID id, String email, String role, String nomComplet) {
        JwtService.TokenGenere token = jwtService.generer(id, email, role, nomComplet);
        return new LoginResponse(token.token(), "Bearer", token.expireLe(),
                id, role, nomComplet);
    }
}

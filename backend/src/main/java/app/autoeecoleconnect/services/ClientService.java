package app.autoeecoleconnect.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.controllers.dto.ClientMiseAJourRequest;
import app.autoeecoleconnect.exceptions.EmailDejaUtiliseException;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.repositories.ClientRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final QuotaService quotaService;
    private final ContexteAutoEcole contexteAutoEcole;

    public ClientService(ClientRepository clientRepository, PasswordEncoder passwordEncoder,
                         QuotaService quotaService, ContexteAutoEcole contexteAutoEcole) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.quotaService = quotaService;
        this.contexteAutoEcole = contexteAutoEcole;
    }

    @Transactional(readOnly = true)
    public List<Client> lister() {
        return clientRepository.findByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante());
    }

    /**
     * 404 et non 403 sur un client d'une autre agence : ne pas révéler
     * l'existence d'une ressource à qui n'y a pas droit, même par la
     * différence entre les deux erreurs (même règle que pour les rôles
     * MONITEUR et CLIENT, docs/12 §12.5).
     */
    @Transactional(readOnly = true)
    public Client trouver(UUID id) {
        return clientRepository.findByIdAndActiveTrueAndAutoEcoleId(id, contexteAutoEcole.courante())
                .orElseThrow(() -> new RessourceIntrouvableException("Client", id));
    }

    public Client creer(ClientCreationRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }
        quotaService.verifierPeutAjouterEleve();
        Client client = new Client();
        client.setNom(request.nom());
        client.setPrenom(request.prenom());
        client.setEmail(request.email());
        client.setPasswordHash(passwordEncoder.encode(request.motDePasse()));
        client.setTelephone(request.telephone());
        client.setAdresse(request.adresse());
        client.setNotes(request.notes());
        client.setAutoEcoleId(contexteAutoEcole.courante());
        return clientRepository.save(client);
    }

    public Client mettreAJour(UUID id, ClientMiseAJourRequest request) {
        Client client = trouver(id);
        if (!client.getEmail().equals(request.email())
                && clientRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }
        client.setNom(request.nom());
        client.setPrenom(request.prenom());
        client.setEmail(request.email());
        client.setTelephone(request.telephone());
        client.setAdresse(request.adresse());
        client.setNotes(request.notes());
        return clientRepository.save(client);
    }

    // Soft delete : le client disparaît des listes mais reste en base
    // (historique des réservations, obligations comptables).
    public void supprimer(UUID id) {
        Client client = trouver(id);
        client.setActive(false);
        clientRepository.save(client);
    }

    /**
     * Droit à l'effacement RGPD (docs/12 §12.6) : anonymisation, pas
     * suppression. Les réservations et séances de l'élève portent un
     * historique comptable que l'auto-école doit conserver — on garde donc la
     * ligne mais on efface toute donnée personnelle et on rend le login
     * définitivement impossible. Même approche que
     * {@code TrialLifecycleScheduler.anonymiser()} côté organisation, à une
     * différence près : {@code password_hash} est NOT NULL, on ne peut pas le
     * mettre à null — on le remplace par le hash d'un secret aléatoire, qu'aucun
     * mot de passe connu ne peut retrouver. {@link #trouver(UUID)} borne
     * l'opération à l'agence courante et refuse un client déjà anonymisé
     * (active=false → 404), donc l'action est idempotente-sûre et irréversible.
     */
    public Client anonymiser(UUID id) {
        Client client = trouver(id);
        client.setNom("[supprimé]");
        client.setPrenom("[supprimé]");
        client.setEmail("supprime-" + client.getId() + "@anonyme.invalid");
        client.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        client.setTelephone(null);
        client.setAdresse(null);
        client.setNotes(null);
        client.setActive(false);
        return clientRepository.save(client);
    }
}

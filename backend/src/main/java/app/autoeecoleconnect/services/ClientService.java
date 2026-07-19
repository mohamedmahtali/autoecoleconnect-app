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

    public ClientService(ClientRepository clientRepository, PasswordEncoder passwordEncoder,
                         QuotaService quotaService) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.quotaService = quotaService;
    }

    @Transactional(readOnly = true)
    public List<Client> lister() {
        return clientRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Client trouver(UUID id) {
        return clientRepository.findByIdAndActiveTrue(id)
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
}

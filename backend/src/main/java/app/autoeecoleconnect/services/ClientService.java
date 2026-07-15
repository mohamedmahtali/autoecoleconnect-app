package app.autoeecoleconnect.services;

import java.util.List;

import app.autoeecoleconnect.controllers.dto.ClientRequest;
import app.autoeecoleconnect.exceptions.EmailDejaUtiliseException;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.StatutClient;
import app.autoeecoleconnect.repositories.ClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Transactional(readOnly = true)
    public List<Client> lister() {
        return clientRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Client trouver(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Client", id));
    }

    public Client creer(ClientRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }
        Client client = new Client();
        appliquer(client, request);
        return clientRepository.save(client);
    }

    public Client mettreAJour(Long id, ClientRequest request) {
        Client client = trouver(id);
        if (!client.getEmail().equals(request.email())
                && clientRepository.existsByEmail(request.email())) {
            throw new EmailDejaUtiliseException(request.email());
        }
        appliquer(client, request);
        return clientRepository.save(client);
    }

    public void supprimer(Long id) {
        Client client = trouver(id);
        clientRepository.delete(client);
    }

    private void appliquer(Client client, ClientRequest request) {
        client.setNom(request.nom());
        client.setPrenom(request.prenom());
        client.setEmail(request.email());
        client.setTelephone(request.telephone());
        client.setDateNaissance(request.dateNaissance());
        client.setStatut(request.statut() != null ? request.statut() : StatutClient.PROSPECT);
    }
}

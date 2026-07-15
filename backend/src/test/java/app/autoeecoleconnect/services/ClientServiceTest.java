package app.autoeecoleconnect.services;

import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.controllers.dto.ClientCreationRequest;
import app.autoeecoleconnect.exceptions.EmailDejaUtiliseException;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ClientService clientService;

    private ClientCreationRequest requeteValide() {
        return new ClientCreationRequest("Dupont", "Marie", "marie.dupont@example.fr",
                "motdepasse-solide", "0612345678", "12 rue des Lilas, Lyon", null);
    }

    @Test
    void creer_hache_le_mot_de_passe_et_enregistre_le_client() {
        when(clientRepository.existsByEmail("marie.dupont@example.fr")).thenReturn(false);
        when(passwordEncoder.encode("motdepasse-solide")).thenReturn("$2a$10$hash");
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client client = clientService.creer(requeteValide());

        assertThat(client.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(client.isActive()).isTrue();
        verify(clientRepository).save(any(Client.class));
    }

    @Test
    void creer_refuse_un_email_deja_utilise() {
        when(clientRepository.existsByEmail("marie.dupont@example.fr")).thenReturn(true);

        assertThatThrownBy(() -> clientService.creer(requeteValide()))
                .isInstanceOf(EmailDejaUtiliseException.class)
                .hasMessageContaining("marie.dupont@example.fr");
        verify(clientRepository, never()).save(any());
    }

    @Test
    void trouver_leve_une_exception_si_le_client_est_introuvable() {
        UUID id = UUID.randomUUID();
        when(clientRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.trouver(id))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void supprimer_desactive_le_client_sans_le_supprimer_de_la_base() {
        UUID id = UUID.randomUUID();
        Client client = new Client();
        client.setActive(true);
        when(clientRepository.findByIdAndActiveTrue(id)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        clientService.supprimer(id);

        assertThat(client.isActive()).isFalse();
        verify(clientRepository).save(client);
        verify(clientRepository, never()).delete(any());
    }
}

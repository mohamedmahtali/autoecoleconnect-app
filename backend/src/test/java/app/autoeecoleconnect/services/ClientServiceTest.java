package app.autoeecoleconnect.services;

import java.time.LocalDate;
import java.util.Optional;

import app.autoeecoleconnect.controllers.dto.ClientRequest;
import app.autoeecoleconnect.exceptions.EmailDejaUtiliseException;
import app.autoeecoleconnect.exceptions.RessourceIntrouvableException;
import app.autoeecoleconnect.models.Client;
import app.autoeecoleconnect.models.StatutClient;
import app.autoeecoleconnect.repositories.ClientRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private ClientService clientService;

    private ClientRequest requeteValide() {
        return new ClientRequest("Dupont", "Marie", "marie.dupont@example.fr",
                "0612345678", LocalDate.of(2004, 5, 12), null);
    }

    @Test
    void creer_enregistre_le_client_avec_statut_prospect_par_defaut() {
        when(clientRepository.existsByEmail("marie.dupont@example.fr")).thenReturn(false);
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        Client client = clientService.creer(requeteValide());

        assertThat(client.getNom()).isEqualTo("Dupont");
        assertThat(client.getStatut()).isEqualTo(StatutClient.PROSPECT);
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
        when(clientRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.trouver(42L))
                .isInstanceOf(RessourceIntrouvableException.class)
                .hasMessageContaining("42");
    }
}

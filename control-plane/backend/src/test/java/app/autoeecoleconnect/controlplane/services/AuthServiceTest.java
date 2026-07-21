package app.autoeecoleconnect.controlplane.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import app.autoeecoleconnect.controlplane.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controlplane.exceptions.IdentifiantsInvalidesException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private JwtService jwtService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private AuthService authService;
    private Organisation organisation;

    @BeforeEach
    void setUp() {
        authService = new AuthService(organisationRepository, passwordEncoder, jwtService, "", "");

        organisation = new Organisation();
        organisation.setNom("Auto-École Test");
        organisation.setEmailGerant("gerant@test.fr");
        organisation.setMotDePasseHash(passwordEncoder.encode("Secret123!"));
    }

    @Test
    void loginAvecBonsIdentifiantsRetourneUnToken() {
        when(organisationRepository.findByEmailGerant("gerant@test.fr"))
                .thenReturn(Optional.of(organisation));
        when(jwtService.generer(organisation.getId(), "gerant@test.fr", "Auto-École Test"))
                .thenReturn(new JwtService.TokenGenere("jwt-token", Instant.now()));

        LoginResponse reponse = authService.login(new LoginRequest("gerant@test.fr", "Secret123!"));

        assertThat(reponse.token()).isEqualTo("jwt-token");
        assertThat(reponse.type()).isEqualTo("Bearer");
        assertThat(reponse.nomOrganisation()).isEqualTo("Auto-École Test");
    }

    @Test
    void loginAvecMauvaisMotDePasseEstRejete() {
        when(organisationRepository.findByEmailGerant("gerant@test.fr"))
                .thenReturn(Optional.of(organisation));

        assertThrows(IdentifiantsInvalidesException.class,
                () -> authService.login(new LoginRequest("gerant@test.fr", "mauvais")));
    }

    @Test
    void loginAvecEmailInconnuEstRejete() {
        when(organisationRepository.findByEmailGerant("inconnu@test.fr"))
                .thenReturn(Optional.empty());

        assertThrows(IdentifiantsInvalidesException.class,
                () -> authService.login(new LoginRequest("inconnu@test.fr", "Secret123!")));
    }

    @Test
    void loginSansHashDefiniEstRejete() {
        // Organisation créée avant la Slice B (mot_de_passe_hash NULL)
        organisation.setMotDePasseHash(null);
        when(organisationRepository.findByEmailGerant("gerant@test.fr"))
                .thenReturn(Optional.of(organisation));

        assertThrows(IdentifiantsInvalidesException.class,
                () -> authService.login(new LoginRequest("gerant@test.fr", "Secret123!")));
    }

    @Test
    void loginSuperadminAvecBonsIdentifiantsRetourneUnTokenSuperadmin() {
        AuthService avecSuperadmin = new AuthService(organisationRepository, passwordEncoder, jwtService,
                "admin@autoecoleconnect.fr", passwordEncoder.encode("SuperSecret1!"));
        when(jwtService.genererSuperAdmin("admin@autoecoleconnect.fr"))
                .thenReturn(new JwtService.TokenGenere("jwt-superadmin", Instant.now()));

        LoginResponse reponse = avecSuperadmin.login(
                new LoginRequest("admin@autoecoleconnect.fr", "SuperSecret1!"));

        assertThat(reponse.token()).isEqualTo("jwt-superadmin");
        assertThat(reponse.nomOrganisation()).isEqualTo("Super Admin");
    }

    @Test
    void loginSuperadminAvecMauvaisMotDePasseEstRejete() {
        AuthService avecSuperadmin = new AuthService(organisationRepository, passwordEncoder, jwtService,
                "admin@autoecoleconnect.fr", passwordEncoder.encode("SuperSecret1!"));

        assertThrows(IdentifiantsInvalidesException.class,
                () -> avecSuperadmin.login(new LoginRequest("admin@autoecoleconnect.fr", "mauvais")));
    }

    @Test
    void superadminNonConfigureNeCourtCircuitePasLeLoginGerant() {
        // Config vide (cas par défaut, authService du @BeforeEach) : un email
        // qui ressemblerait à un superadmin retombe normalement sur la
        // recherche d'organisation, sans jamais planter sur une chaîne vide.
        when(organisationRepository.findByEmailGerant("gerant@test.fr"))
                .thenReturn(Optional.of(organisation));
        when(jwtService.generer(organisation.getId(), "gerant@test.fr", "Auto-École Test"))
                .thenReturn(new JwtService.TokenGenere("jwt-token", Instant.now()));

        LoginResponse reponse = authService.login(new LoginRequest("gerant@test.fr", "Secret123!"));

        assertThat(reponse.nomOrganisation()).isEqualTo("Auto-École Test");
    }
}

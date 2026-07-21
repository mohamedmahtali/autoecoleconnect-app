package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controlplane.exceptions.IdentifiantsInvalidesException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login gérant : un compte = une organisation (email_gerant). Même stack que
 * l'AuthService du backend tenant (BCrypt + JWT HS256 stateless).
 *
 * <p>Version minimale du super-admin (docs/16-backlog.md §16.3 item 17) :
 * pas de nouvelle entité (aucune organisation à associer), identifiants
 * purement config — email + hash BCrypt vides par défaut = désactivé, même
 * logique que {@code StripeProperties.estConfigure()} côté tenant.</p>
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    // Sentinelle : le super-admin n'a pas d'organisation, mais LoginResponse
    // exige un UUID — aucun endpoint /api/admin/** n'en a l'usage.
    private static final UUID ORGANISATION_ID_SUPERADMIN = new UUID(0, 0);

    private final OrganisationRepository organisationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String superadminEmail;
    private final String superadminPasswordHash;

    public AuthService(OrganisationRepository organisationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.superadmin.email:}") String superadminEmail,
                       @Value("${app.superadmin.password-hash:}") String superadminPasswordHash) {
        this.organisationRepository = organisationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.superadminEmail = superadminEmail;
        this.superadminPasswordHash = superadminPasswordHash;
    }

    public LoginResponse login(LoginRequest request) {
        if (estSuperadminConfigure() && superadminEmail.equals(request.email())) {
            if (!passwordEncoder.matches(request.motDePasse(), superadminPasswordHash)) {
                throw new IdentifiantsInvalidesException();
            }
            JwtService.TokenGenere token = jwtService.genererSuperAdmin(superadminEmail);
            return new LoginResponse(token.token(), "Bearer", token.expireLe(),
                    ORGANISATION_ID_SUPERADMIN, "Super Admin");
        }

        Organisation organisation = organisationRepository.findByEmailGerant(request.email())
                .orElseThrow(IdentifiantsInvalidesException::new);

        // Hash absent = organisation créée avant la Slice B : login impossible
        // tant qu'aucun mot de passe n'est défini.
        if (organisation.getMotDePasseHash() == null
                || !passwordEncoder.matches(request.motDePasse(), organisation.getMotDePasseHash())) {
            throw new IdentifiantsInvalidesException();
        }

        JwtService.TokenGenere token = jwtService.generer(
                organisation.getId(), organisation.getEmailGerant(), organisation.getNom());
        return new LoginResponse(token.token(), "Bearer", token.expireLe(),
                organisation.getId(), organisation.getNom());
    }

    private boolean estSuperadminConfigure() {
        return !superadminEmail.isBlank() && !superadminPasswordHash.isBlank();
    }
}

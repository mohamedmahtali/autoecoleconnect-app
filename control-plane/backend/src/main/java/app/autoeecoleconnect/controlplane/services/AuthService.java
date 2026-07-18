package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.controllers.dto.LoginRequest;
import app.autoeecoleconnect.controlplane.controllers.dto.LoginResponse;
import app.autoeecoleconnect.controlplane.exceptions.IdentifiantsInvalidesException;
import app.autoeecoleconnect.controlplane.models.Organisation;
import app.autoeecoleconnect.controlplane.repositories.OrganisationRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Login gérant : un compte = une organisation (email_gerant). Même stack que
 * l'AuthService du backend tenant (BCrypt + JWT HS256 stateless).
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final OrganisationRepository organisationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(OrganisationRepository organisationRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.organisationRepository = organisationRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request) {
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
}

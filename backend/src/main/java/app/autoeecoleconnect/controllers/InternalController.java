package app.autoeecoleconnect.controllers;

import app.autoeecoleconnect.controllers.dto.AutoEcoleCreationInterneRequest;
import app.autoeecoleconnect.controllers.dto.AutoEcoleResponse;
import app.autoeecoleconnect.controllers.dto.JetonAccesInterneRequest;
import app.autoeecoleconnect.controllers.dto.LoginResponse;
import app.autoeecoleconnect.models.AutoEcole;
import app.autoeecoleconnect.services.AuthentificationInterne;
import app.autoeecoleconnect.services.AutoEcoleService;
import app.autoeecoleconnect.services.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Routes réservées au control-plane, qui n'a aucun JWT tenant à présenter.
 * Déclarées {@code permitAll} dans {@code SecurityConfig} : l'autorisation se
 * fait ici, à la main, sur le secret partagé {@code X-Internal-Api-Key}
 * (docs/18 §18.3 lots 4 et 5).
 *
 * <p>⚠️ Ces routes sont joignables depuis l'extérieur comme toutes les
 * autres. Le seul rempart est le secret ; tout ajout ici doit donc rester
 * strictement nécessaire au control-plane et sans effet destructeur.
 */
@RestController
@RequestMapping("/api/internal")
@Tag(name = "Interne", description = "Appels du control-plane (secret partagé)")
public class InternalController {

    private final AutoEcoleService autoEcoleService;
    private final JwtService jwtService;
    private final AuthentificationInterne authentificationInterne;

    public InternalController(AutoEcoleService autoEcoleService,
                              JwtService jwtService,
                              AuthentificationInterne authentificationInterne) {
        this.autoEcoleService = autoEcoleService;
        this.jwtService = jwtService;
        this.authentificationInterne = authentificationInterne;
    }

    @Operation(summary = "Créer une agence dans cette organisation (control-plane)")
    @PostMapping("/auto-ecoles")
    public AutoEcoleResponse creerAgence(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String cle,
            @Valid @RequestBody AutoEcoleCreationInterneRequest request) {
        exigerAppelInterne(cle);
        AutoEcole agence = autoEcoleService.creer(request.nom(), request.slug(), request.adresse());
        return AutoEcoleResponse.depuis(agence);
    }

    /**
     * Émet un jeton tenant pour le gérant, que le control-plane a déjà
     * authentifié de son côté (docs/18 §18.3 lot 5).
     *
     * <p>Le jeton porte le rôle DIRECTEUR sur l'agence demandée, plutôt qu'un
     * rôle « gérant » distinct : le gérant a de fait tous les droits sur ses
     * agences, et cela évite de dupliquer toute la matrice d'autorisation
     * pour un second rôle qui aurait exactement les mêmes permissions. Le
     * périmètre reste une agence à la fois — le gérant en change en
     * redemandant un jeton, ce qui préserve la règle « toute lecture porte
     * une agence ».
     */
    @Operation(summary = "Émettre un jeton d'accès pour le gérant (control-plane)")
    @PostMapping("/jeton-acces")
    public LoginResponse jetonAcces(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String cle,
            @Valid @RequestBody JetonAccesInterneRequest request) {
        exigerAppelInterne(cle);
        // Vérifie que l'agence demandée existe bien dans cette organisation :
        // sans ce contrôle, un identifiant erroné donnerait un jeton portant un
        // périmètre inexistant, donc des listes vides inexplicables.
        AutoEcole agence = autoEcoleService.trouver(request.autoEcoleId());
        JwtService.TokenGenere token = jwtService.generer(
                agence.getId(), request.email(), "DIRECTEUR", request.nomComplet(), agence.getId());
        return new LoginResponse(token.token(), "Bearer", token.expireLe(),
                agence.getId(), "DIRECTEUR", request.nomComplet());
    }

    private void exigerAppelInterne(String cle) {
        if (!authentificationInterne.estValide(cle)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
    }
}

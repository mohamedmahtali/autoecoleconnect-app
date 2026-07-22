package app.autoeecoleconnect.controlplane.services;

import java.util.List;
import java.util.UUID;

import app.autoeecoleconnect.controlplane.exceptions.ProvisioningException;
import app.autoeecoleconnect.controlplane.models.AutoEcole;
import app.autoeecoleconnect.controlplane.models.Tenant;
import app.autoeecoleconnect.controlplane.repositories.AutoEcoleRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Création d'une agence dans une organisation existante (docs/18 §18.3 lot 4)
 * — le parcours qui manquait pour qu'une organisation puisse être
 * multi-agences.
 */
@Service
public class AutoEcoleService {

    private static final Logger log = LoggerFactory.getLogger(AutoEcoleService.class);

    private final AutoEcoleRepository autoEcoleRepository;
    private final TenantRepository tenantRepository;
    private final SlugService slugService;
    private final TenantInterneClient tenantInterneClient;
    private final GitHubService gitHubService;

    public AutoEcoleService(AutoEcoleRepository autoEcoleRepository,
                            TenantRepository tenantRepository,
                            SlugService slugService,
                            TenantInterneClient tenantInterneClient,
                            GitHubService gitHubService) {
        this.autoEcoleRepository = autoEcoleRepository;
        this.tenantRepository = tenantRepository;
        this.slugService = slugService;
        this.tenantInterneClient = tenantInterneClient;
        this.gitHubService = gitHubService;
    }

    public List<AutoEcole> listerPour(UUID tenantId) {
        return autoEcoleRepository.findByTenantIdOrderByNom(tenantId);
    }

    /**
     * ⚠️ Contrôle de propriété, à appeler avant toute action du gérant sur un
     * tenant : l'identifiant vient du corps de la requête, rien n'empêcherait
     * sinon un gérant de désigner le tenant d'une autre organisation.
     * Introuvable plutôt qu'interdit, pour ne pas révéler son existence.
     */
    public Tenant exigerTenantDeLOrganisation(UUID tenantId, UUID organisationId) {
        return tenantRepository.findById(tenantId)
                .filter(t -> t.getOrganisation() != null
                        && organisationId.equals(t.getOrganisation().getId()))
                .orElseThrow(() -> new ProvisioningException(
                        "Tenant introuvable pour cette organisation : " + tenantId, null));
    }

    /**
     * Créer une agence a trois effets, dans cet ordre <b>impératif</b> :
     * <ol>
     *   <li>l'agence est créée <b>dans la base du tenant</b>, via le canal
     *       interne — c'est là que vivront ses élèves et ses moniteurs ;</li>
     *   <li>elle est enregistrée ici, pour l'unicité du slug et la
     *       composition du values.yaml ;</li>
     *   <li>le sous-domaine est publié dans le values.yaml GitOps.</li>
     * </ol>
     *
     * <p>⚠️ Publier l'URL avant que l'agence n'existe côté tenant exposerait
     * un sous-domaine qui répond mais ne correspond à rien — visible par des
     * élèves. L'ordre inverse ne coûte, au pire, qu'une agence créée sans URL
     * encore publiée, rattrapable en rejouant l'appel (les deux étapes sont
     * idempotentes sur le slug).
     */
    public AutoEcole creer(Tenant tenant, String nom, String adresse) {
        UUID tenantId = tenant.getId();
        String slug = slugService.genererSlugUnique(nom);

        tenantInterneClient.creerAutoEcole(tenant.getNamespace(), nom, slug, adresse);

        AutoEcole agence = new AutoEcole();
        agence.setTenant(tenant);
        agence.setNom(nom);
        agence.setSlug(slug);
        AutoEcole enregistree = autoEcoleRepository.save(agence);

        List<String> slugs = autoEcoleRepository.findByTenantIdOrderByNom(tenantId)
                .stream().map(AutoEcole::getSlug).toList();
        gitHubService.mettreAJourAutoEcoles(tenant.getSlug(), slugs);

        log.info("Agence {} ({}) créée dans le tenant {}", nom, slug, tenant.getSlug());
        return enregistree;
    }

    /**
     * Ouvre une session sur une agence sans que le gérant ait à se
     * réauthentifier (docs/18 §18.3 lot 5, item 38 du backlog).
     *
     * <p><b>Pourquoi le tenant émet le jeton et non le control-plane</b> :
     * les deux mondes ont des clés de signature distinctes, et c'est une
     * bonne chose — un jeton du control-plane ne doit pas ouvrir un tenant.
     * Plutôt que de partager une clé ou d'apprendre au tenant à valider deux
     * émetteurs, le control-plane authentifie le gérant de son côté puis
     * <b>demande</b> au tenant, par le canal interne déjà en place, d'émettre
     * un jeton de son propre cru. Aucune clé ne traverse la frontière.
     *
     * <p>Le périmètre reste une agence à la fois : changer d'agence, c'est
     * redemander un jeton. La règle « toute lecture porte une agence » n'est
     * donc jamais contournée, même pour le gérant.
     */
    public AccesAgence ouvrirAcces(UUID autoEcoleId, UUID organisationId,
                                   String emailGerant, String nomGerant, String domaine) {
        AutoEcole agence = autoEcoleRepository.findById(autoEcoleId)
                .orElseThrow(() -> new ProvisioningException(
                        "Agence introuvable : " + autoEcoleId, null));
        Tenant tenant = exigerTenantDeLOrganisation(agence.getTenant().getId(), organisationId);

        var jeton = tenantInterneClient.jetonAcces(
                tenant.getNamespace(), emailGerant, nomGerant, agence.getId());

        return new AccesAgence(jeton.token(), jeton.type(), agence.getSlug() + "." + domaine);
    }

    public record AccesAgence(String token, String type, String url) {
    }
}

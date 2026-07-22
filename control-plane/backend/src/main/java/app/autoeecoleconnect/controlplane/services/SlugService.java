package app.autoeecoleconnect.controlplane.services;

import app.autoeecoleconnect.controlplane.repositories.AutoEcoleRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import java.text.Normalizer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SlugService {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern DASH_EDGES = Pattern.compile("^-+|-+$");

    private final TenantRepository tenantRepository;
    private final AutoEcoleRepository autoEcoleRepository;

    public SlugService(TenantRepository tenantRepository, AutoEcoleRepository autoEcoleRepository) {
        this.tenantRepository = tenantRepository;
        this.autoEcoleRepository = autoEcoleRepository;
    }

    /**
     * Slugifie {@code nom} et garantit l'unicité en base en suffixant
     * -2, -3... sur collision (docs/06 §6.2 : tenants.slug UNIQUE).
     */
    public String genererSlugUnique(String nom) {
        String base = slugifier(nom);
        String candidat = base;
        int suffixe = 2;
        while (dejaPris(candidat)) {
            candidat = base + "-" + suffixe;
            suffixe++;
        }
        return candidat;
    }

    /**
     * Un slug est un sous-domaine public : il doit être unique sur l'ensemble
     * du domaine, donc face aux tenants <b>et</b> aux agences (docs/18 §18.3
     * lot 4). Vérifier les seuls tenants laisserait deux agences de deux
     * organisations différentes revendiquer la même URL.
     */
    private boolean dejaPris(String candidat) {
        return tenantRepository.existsBySlug(candidat) || autoEcoleRepository.existsBySlug(candidat);
    }

    private String slugifier(String nom) {
        String normalise = Normalizer.normalize(nom, Normalizer.Form.NFD);
        String sansAccents = DIACRITICS.matcher(normalise).replaceAll("");
        String minuscule = sansAccents.toLowerCase();
        String tirets = NON_ALPHANUM.matcher(minuscule).replaceAll("-");
        String slug = DASH_EDGES.matcher(tirets).replaceAll("");
        return slug.isBlank() ? "auto-ecole" : slug;
    }
}

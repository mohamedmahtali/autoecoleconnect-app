package app.autoeecoleconnect.controlplane.services;

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

    public SlugService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /**
     * Slugifie {@code nom} et garantit l'unicité en base en suffixant
     * -2, -3... sur collision (docs/06 §6.2 : tenants.slug UNIQUE).
     */
    public String genererSlugUnique(String nom) {
        String base = slugifier(nom);
        String candidat = base;
        int suffixe = 2;
        while (tenantRepository.existsBySlug(candidat)) {
            candidat = base + "-" + suffixe;
            suffixe++;
        }
        return candidat;
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

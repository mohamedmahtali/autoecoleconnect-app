package app.autoeecoleconnect.controlplane.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import app.autoeecoleconnect.controlplane.repositories.AutoEcoleRepository;
import app.autoeecoleconnect.controlplane.repositories.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlugServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private AutoEcoleRepository autoEcoleRepository;

    @Test
    void slugifieEtRetireLesAccents() {
        SlugService service = new SlugService(tenantRepository, autoEcoleRepository);

        String slug = service.genererSlugUnique("Auto-École Test Marseille");

        assertThat(slug).isEqualTo("auto-ecole-test-marseille");
    }

    @Test
    void suffixeSurCollision() {
        when(tenantRepository.existsBySlug("auto-ecole-lyon")).thenReturn(true);
        when(tenantRepository.existsBySlug("auto-ecole-lyon-2")).thenReturn(false);
        SlugService service = new SlugService(tenantRepository, autoEcoleRepository);

        String slug = service.genererSlugUnique("Auto École Lyon");

        assertThat(slug).isEqualTo("auto-ecole-lyon-2");
    }

    /**
     * Un slug d'agence est un sous-domaine public au même titre qu'un slug de
     * tenant : la collision doit être détectée entre les deux tables, sinon
     * deux organisations revendiqueraient la même URL.
     */
    @Test
    void suffixeSurCollisionAvecUneAgenceDUneAutreOrganisation() {
        when(tenantRepository.existsBySlug("agence-bron")).thenReturn(false);
        when(autoEcoleRepository.existsBySlug("agence-bron")).thenReturn(true);
        when(tenantRepository.existsBySlug("agence-bron-2")).thenReturn(false);
        when(autoEcoleRepository.existsBySlug("agence-bron-2")).thenReturn(false);
        SlugService service = new SlugService(tenantRepository, autoEcoleRepository);

        assertThat(service.genererSlugUnique("Agence Bron")).isEqualTo("agence-bron-2");
    }
}

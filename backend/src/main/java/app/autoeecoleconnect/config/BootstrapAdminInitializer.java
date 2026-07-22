package app.autoeecoleconnect.config;

import java.util.UUID;

import app.autoeecoleconnect.models.Directeur;
import app.autoeecoleconnect.repositories.DirecteurRepository;
import app.autoeecoleconnect.services.ContexteAutoEcole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Crée le premier compte directeur au démarrage si la table est vide
 * (identifiants fournis par ADMIN_EMAIL / ADMIN_PASSWORD). En production,
 * le Provisioning Service du Control Plane fournira ces valeurs par tenant.
 *
 * <p>Le directeur est rattaché à l'agence par défaut, celle que la migration
 * v1.1 crée à partir du slug du tenant (docs/18 §18.3). 🔜 Quand une
 * organisation pourra avoir plusieurs agences et plusieurs directeurs
 * (lot 2), la condition « la table est vide » devra devenir « cette agence
 * n'a pas encore de directeur ».
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final DirecteurRepository directeurRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContexteAutoEcole contexteAutoEcole;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapAdminInitializer(DirecteurRepository directeurRepository,
                                     PasswordEncoder passwordEncoder,
                                     ContexteAutoEcole contexteAutoEcole,
                                     @Value("${app.bootstrap.admin-email}") String adminEmail,
                                     @Value("${app.bootstrap.admin-password}") String adminPassword) {
        this.directeurRepository = directeurRepository;
        this.passwordEncoder = passwordEncoder;
        this.contexteAutoEcole = contexteAutoEcole;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Par agence et non « si la table est vide » : une organisation peut
        // avoir plusieurs agences, chacune doit pouvoir recevoir son directeur
        // de bootstrap sans que la présence d'un directeur ailleurs l'en prive.
        UUID agence = contexteAutoEcole.courante();
        if (directeurRepository.countByActiveTrueAndAutoEcoleId(agence) > 0) {
            return;
        }
        Directeur admin = new Directeur();
        admin.setNom("Bootstrap");
        admin.setPrenom("Admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setAutoEcoleId(agence);
        directeurRepository.save(admin);
        log.warn("Compte directeur bootstrap créé : {} — changez le mot de passe", adminEmail);
    }
}

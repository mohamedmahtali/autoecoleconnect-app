package app.autoeecoleconnect.config;

import app.autoeecoleconnect.models.Directeur;
import app.autoeecoleconnect.repositories.DirecteurRepository;
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
 */
@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final DirecteurRepository directeurRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public BootstrapAdminInitializer(DirecteurRepository directeurRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${app.bootstrap.admin-email}") String adminEmail,
                                     @Value("${app.bootstrap.admin-password}") String adminPassword) {
        this.directeurRepository = directeurRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (directeurRepository.count() > 0) {
            return;
        }
        Directeur admin = new Directeur();
        admin.setNom("Bootstrap");
        admin.setPrenom("Admin");
        admin.setEmail(adminEmail);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        directeurRepository.save(admin);
        log.warn("Compte directeur bootstrap créé : {} — changez le mot de passe", adminEmail);
    }
}

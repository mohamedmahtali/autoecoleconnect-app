package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Directeur;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ⚠️ Même règle que les autres repositories : toute lecture porte
 * {@code AutoEcoleId} (docs/18 §18.3). {@link #findByEmailAndActiveTrue} et
 * {@link #existsByEmail} font exception, l'email étant unique globalement
 * dans la base et le login ne connaissant pas encore l'agence au moment où
 * il cherche le compte.
 */
public interface DirecteurRepository extends JpaRepository<Directeur, UUID> {

    Optional<Directeur> findByEmailAndActiveTrue(String email);

    List<Directeur> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    Optional<Directeur> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);

    long countByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    boolean existsByEmail(String email);
}

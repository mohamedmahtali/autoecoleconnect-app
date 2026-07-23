package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.DisponibiliteMoniteur;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ⚠️ Toute lecture porte {@code AutoEcoleId} — isolation entre agences (docs/18
 * §18.3 lot 1).
 *
 * <p>{@code moniteur} est LAZY et open-in-view est désactivé : les finders
 * mappés en DTO le chargent via {@link EntityGraph} (sinon
 * LazyInitializationException au mapping, cf. ReservationRepository /
 * ExamenRepository). La somme d'heures du KPI n'y touche pas.
 */
public interface DisponibiliteMoniteurRepository extends JpaRepository<DisponibiliteMoniteur, UUID> {

    @EntityGraph(attributePaths = {"moniteur"})
    List<DisponibiliteMoniteur> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    @EntityGraph(attributePaths = {"moniteur"})
    Optional<DisponibiliteMoniteur> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);

    // Total des heures disponibles déclarées par semaine dans l'agence
    // (dénominateur du taux d'occupation, #35). Requête native : l'arithmétique
    // sur TIME (heure_fin - heure_debut -> interval -> heures) n'a pas
    // d'équivalent JPQL portable. Filtre par école explicite, comme les autres
    // requêtes natives (voir ClientRepository.inscriptionsParMois).
    @Query(value = """
            SELECT COALESCE(SUM(EXTRACT(EPOCH FROM (heure_fin - heure_debut)) / 3600), 0)
            FROM disponibilites_moniteur
            WHERE active = true AND auto_ecole_id = :autoEcoleId
            """, nativeQuery = true)
    double sommeHeuresHebdo(@Param("autoEcoleId") UUID autoEcoleId);
}

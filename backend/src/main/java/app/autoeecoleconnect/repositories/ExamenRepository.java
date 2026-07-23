package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Examen;
import app.autoeecoleconnect.models.ResultatExamen;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ⚠️ Toute méthode de lecture porte {@code AutoEcoleId} — c'est la garantie
 * d'isolation entre agences (docs/18 §18.3 lot 1). Ne pas ajouter de variante
 * sans école : un appelant qui oublie le périmètre doit se heurter à une
 * erreur de compilation, pas lire les données d'une autre agence.
 *
 * <p>{@code client} est LAZY et open-in-view est désactivé : les finders
 * mappés en DTO le chargent dans la même requête via {@link EntityGraph},
 * sinon LazyInitializationException au mapping (même raison que
 * ReservationRepository). Le comptage du KPI n'y touche pas, il s'en passe.
 */
public interface ExamenRepository extends JpaRepository<Examen, UUID> {

    @EntityGraph(attributePaths = {"client"})
    List<Examen> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    @EntityGraph(attributePaths = {"client"})
    Optional<Examen> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);

    @EntityGraph(attributePaths = {"client"})
    List<Examen> findByActiveTrueAndAutoEcoleIdAndClientId(UUID autoEcoleId, UUID clientId);

    // KPI taux de réussite (StatsService) : appelé pour REUSSI puis ECHOUE,
    // toujours borné à l'agence courante.
    long countByActiveTrueAndAutoEcoleIdAndResultat(UUID autoEcoleId, ResultatExamen resultat);
}

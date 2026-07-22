package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * ⚠️ Toute méthode de lecture porte {@code AutoEcoleId} — c'est la garantie
 * d'isolation entre agences (docs/18 §18.3 lot 1). Ne pas ajouter de variante
 * sans école : un appelant qui oublie le périmètre doit se heurter à une
 * erreur de compilation, pas lire les données d'une autre agence.
 *
 * <p>Deux exceptions assumées, toutes deux liées à l'unicité <b>globale</b>
 * de l'email dans la base : {@link #findByEmailAndActiveTrue} sert au login,
 * où l'école n'est pas encore connue, et {@link #existsByEmail} vérifie la
 * contrainte {@code UNIQUE} qui, elle, ignore les écoles.
 */
public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    long countByActiveTrueAndAutoEcoleId(UUID autoEcoleId);

    /**
     * Comptage <b>toutes agences confondues</b>, réservé aux quotas : le plan
     * est vendu à l'organisation, pas à l'école (docs/17 §17.6 décision 2).
     * Écrit en JPQL et nommé explicitement plutôt que dérivé
     * ({@code countByActiveTrue}) pour qu'un appel hors quota saute aux yeux
     * en revue — c'est la seule lecture non filtrée de ce repository.
     */
    @Query("SELECT COUNT(c) FROM Client c WHERE c.active = true")
    long compterActifsToutesEcoles();

    Optional<Client> findByIdAndActiveTrueAndAutoEcoleId(UUID id, UUID autoEcoleId);

    Optional<Client> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);

    // Requête native (date_trunc n'a pas d'équivalent JPQL portable) —
    // inscriptions des 12 derniers mois, docs/16-backlog.md §16.3 item 15.
    // ⚠️ Le filtre par école est explicite ici : une requête native échappe à
    // tout mécanisme JPA. C'est exactement le cas que le plan initial
    // (@Filter Hibernate) n'aurait pas couvert — voir ContexteAutoEcole.
    @Query(value = """
            SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS mois,
                   COUNT(*) AS nombre
            FROM clients
            WHERE auto_ecole_id = :autoEcoleId
              AND created_at >= date_trunc('month', now()) - INTERVAL '11 months'
            GROUP BY date_trunc('month', created_at)
            ORDER BY date_trunc('month', created_at)
            """, nativeQuery = true)
    List<Object[]> inscriptionsParMois(@Param("autoEcoleId") UUID autoEcoleId);
}

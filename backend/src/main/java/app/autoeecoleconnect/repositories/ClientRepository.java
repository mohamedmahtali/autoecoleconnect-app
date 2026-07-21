package app.autoeecoleconnect.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import app.autoeecoleconnect.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    List<Client> findByActiveTrue();

    long countByActiveTrue();

    Optional<Client> findByIdAndActiveTrue(UUID id);

    Optional<Client> findByEmailAndActiveTrue(String email);

    boolean existsByEmail(String email);

    // Requête native (date_trunc n'a pas d'équivalent JPQL portable) —
    // inscriptions des 12 derniers mois, docs/16-backlog.md §16.3 item 15.
    @Query(value = """
            SELECT to_char(date_trunc('month', created_at), 'YYYY-MM') AS mois,
                   COUNT(*) AS nombre
            FROM clients
            WHERE created_at >= date_trunc('month', now()) - INTERVAL '11 months'
            GROUP BY date_trunc('month', created_at)
            ORDER BY date_trunc('month', created_at)
            """, nativeQuery = true)
    List<Object[]> inscriptionsParMois();
}

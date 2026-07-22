package app.autoeecoleconnect.services;

import app.autoeecoleconnect.controllers.dto.StatsResponse;
import app.autoeecoleconnect.models.StatutSeance;
import app.autoeecoleconnect.repositories.ClientRepository;
import app.autoeecoleconnect.repositories.ReservationRepository;
import app.autoeecoleconnect.repositories.SeanceRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StatsService {

    private final ReservationRepository reservationRepository;
    private final ClientRepository clientRepository;
    private final SeanceRepository seanceRepository;

    private final ContexteAutoEcole contexteAutoEcole;

    public StatsService(ReservationRepository reservationRepository,
                        ClientRepository clientRepository,
                        SeanceRepository seanceRepository,
                        ContexteAutoEcole contexteAutoEcole) {
        this.reservationRepository = reservationRepository;
        this.clientRepository = clientRepository;
        this.seanceRepository = seanceRepository;
        this.contexteAutoEcole = contexteAutoEcole;
    }

    /**
     * Chiffres de l'agence courante. 🔜 Le control-plane, lui, interroge cette
     * même route pour obtenir le total de l'organisation : tant qu'une base ne
     * contient qu'une école, les deux coïncident. La distinction devra être
     * traitée quand une organisation en aura plusieurs (docs/18 §18.3 lot 7,
     * remplacement de TenantStatsClient par un GROUP BY).
     */
    public StatsResponse resume() {
        long terminees = seanceRepository.countByActiveTrueAndStatut(StatutSeance.COMPLETED);
        long noShow = seanceRepository.countByActiveTrueAndStatut(StatutSeance.NO_SHOW);
        long seancesCloturees = terminees + noShow;
        double tauxNoShow = seancesCloturees == 0 ? 0.0 : (double) noShow / seancesCloturees;

        List<StatsResponse.InscriptionMensuelle> inscriptions = clientRepository
                .inscriptionsParMois(contexteAutoEcole.courante())
                .stream()
                .map(ligne -> new StatsResponse.InscriptionMensuelle(
                        (String) ligne[0], ((Number) ligne[1]).longValue()))
                .toList();

        return new StatsResponse(
                reservationRepository.sumMontantPaye(),
                clientRepository.countByActiveTrueAndAutoEcoleId(contexteAutoEcole.courante()),
                terminees,
                noShow,
                tauxNoShow,
                inscriptions);
    }
}

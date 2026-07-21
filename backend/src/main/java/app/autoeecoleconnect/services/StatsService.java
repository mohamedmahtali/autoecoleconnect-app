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

    public StatsService(ReservationRepository reservationRepository,
                        ClientRepository clientRepository,
                        SeanceRepository seanceRepository) {
        this.reservationRepository = reservationRepository;
        this.clientRepository = clientRepository;
        this.seanceRepository = seanceRepository;
    }

    public StatsResponse resume() {
        long terminees = seanceRepository.countByActiveTrueAndStatut(StatutSeance.COMPLETED);
        long noShow = seanceRepository.countByActiveTrueAndStatut(StatutSeance.NO_SHOW);
        long seancesCloturees = terminees + noShow;
        double tauxNoShow = seancesCloturees == 0 ? 0.0 : (double) noShow / seancesCloturees;

        List<StatsResponse.InscriptionMensuelle> inscriptions = clientRepository.inscriptionsParMois()
                .stream()
                .map(ligne -> new StatsResponse.InscriptionMensuelle(
                        (String) ligne[0], ((Number) ligne[1]).longValue()))
                .toList();

        return new StatsResponse(
                reservationRepository.sumMontantPaye(),
                clientRepository.countByActiveTrue(),
                terminees,
                noShow,
                tauxNoShow,
                inscriptions);
    }
}

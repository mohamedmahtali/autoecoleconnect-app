package app.autoeecoleconnect.services;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long dureeMinutes;

    public JwtService(JwtEncoder jwtEncoder,
                      @Value("${app.security.jwt-duree-minutes}") long dureeMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.dureeMinutes = dureeMinutes;
    }

    public record TokenGenere(String token, Instant expireLe) {
    }

    /**
     * {@code autoEcoleId} est le périmètre du porteur : l'agence dont il voit
     * les données (docs/18 §18.3 lot 2). Il est posé dans le jeton à
     * l'authentification et relu à chaque requête par
     * {@code FiltrePerimetreAutoEcole}, qui alimente {@link ContexteAutoEcole}.
     */
    public TokenGenere generer(UUID id, String email, String role, String nomComplet, UUID autoEcoleId) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(dureeMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(id.toString())
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .claim("email", email)
                .claim("role", role)
                .claim("nomComplet", nomComplet)
                .claim("autoEcoleId", autoEcoleId.toString())
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenGenere(token, expiration);
    }
}

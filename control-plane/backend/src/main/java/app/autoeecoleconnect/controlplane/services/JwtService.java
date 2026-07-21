package app.autoeecoleconnect.controlplane.services;

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

    // sub = id de l'organisation — un gérant = une organisation (Slice B).
    public TokenGenere generer(UUID organisationId, String email, String nomOrganisation) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(dureeMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(organisationId.toString())
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .claim("email", email)
                .claim("role", "GERANT")
                .claim("nomOrganisation", nomOrganisation)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenGenere(token, expiration);
    }

    // Super-admin (docs/16-backlog.md §16.3 item 17) : pas d'organisation,
    // sub = l'email lui-même (aucun endpoint /api/admin/** n'en fait un UUID).
    public TokenGenere genererSuperAdmin(String email) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(dureeMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(email)
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .claim("email", email)
                .claim("role", "SUPERADMIN")
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenGenere(token, expiration);
    }
}

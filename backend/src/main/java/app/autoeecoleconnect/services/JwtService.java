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

    public TokenGenere generer(UUID id, String email, String role, String nomComplet) {
        Instant maintenant = Instant.now();
        Instant expiration = maintenant.plus(dureeMinutes, ChronoUnit.MINUTES);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(id.toString())
                .issuedAt(maintenant)
                .expiresAt(expiration)
                .claim("email", email)
                .claim("role", role)
                .claim("nomComplet", nomComplet)
                .build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new TokenGenere(token, expiration);
    }
}

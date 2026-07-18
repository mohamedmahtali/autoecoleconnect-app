package app.autoeecoleconnect.controlplane.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import app.autoeecoleconnect.controlplane.AbstractIntegrationTest;
import app.autoeecoleconnect.controlplane.repositories.WebhookEventRepository;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "stripe.webhook-secret=whsec_secret_de_test")
class StripeWebhookControllerIntegrationTest extends AbstractIntegrationTest {

    private static final String SECRET = "whsec_secret_de_test";

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    // Équivalent Java de Webhook.generateTestHeaderValue (inexistant dans le
    // SDK Java) : signature v1 = HMAC-SHA256(secret, "<timestamp>.<payload>").
    private static String signatureStripe(String payload) throws Exception {
        long ts = System.currentTimeMillis() / 1000;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal((ts + "." + payload).getBytes(StandardCharsets.UTF_8));
        return "t=" + ts + ",v1=" + HexFormat.of().formatHex(signature);
    }

    private ResponseEntity<String> poster(String payload, String signature) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (signature != null) {
            headers.set("Stripe-Signature", signature);
        }
        return rest.postForEntity(url("/api/webhooks/stripe"),
                new HttpEntity<>(payload, headers), String.class);
    }

    @Test
    void webhookSigneEstAccepteEtTrace() throws Exception {
        String payload = """
                {"id":"evt_it_1","type":"customer.created","data":{"object":{
                "object":"customer","id":"cus_it_1"}}}
                """;

        ResponseEntity<String> reponse = poster(payload, signatureStripe(payload));

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(webhookEventRepository.existsByStripeEventId("evt_it_1")).isTrue();
    }

    @Test
    void webhookSansSignatureEstRejete() {
        ResponseEntity<String> reponse = poster("{}", null);

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void webhookAvecSignatureInvalideEstRejete() {
        ResponseEntity<String> reponse = poster("{}", "t=123,v1=deadbeef");

        assertThat(reponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

package app.autoeecoleconnect.controlplane.config;

import com.stripe.StripeClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Client Stripe (pattern StripeClient, pas le statique Stripe.apiKey legacy).
 * Clé vide en dev local / CI : le bean existe quand même (clé factice) —
 * CheckoutService refuse alors les checkouts (voir estConfigure()), même
 * bascule douce que le GitHub PAT absent.
 */
@Configuration
public class StripeConfig {

    @Bean
    public StripeClient stripeClient(StripeProperties properties) {
        String cle = properties.estConfigure() ? properties.apiKey() : "sk_test_cle_factice_dev";
        return new StripeClient(cle);
    }
}

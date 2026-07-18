package app.autoeecoleconnect.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String priceSolo,
        String pricePro,
        String priceGroupe,
        String baseUrl) {

    public boolean estConfigure() {
        return apiKey != null && !apiKey.isBlank();
    }
}

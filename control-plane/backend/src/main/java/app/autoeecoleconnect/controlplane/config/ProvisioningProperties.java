package app.autoeecoleconnect.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.provisioning")
public record ProvisioningProperties(
        String domaine,
        String githubOwner,
        String githubRepo,
        String githubPat,
        String resendApiKey,
        String resendFrom,
        String inviteToken,
        long pollIntervalMs,
        long timeoutMinutes,
        long trialDays,
        String argocdNamespace) {
}

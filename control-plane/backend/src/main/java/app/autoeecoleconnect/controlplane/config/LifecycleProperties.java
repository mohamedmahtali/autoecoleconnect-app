package app.autoeecoleconnect.controlplane.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.lifecycle")
public record LifecycleProperties(
        String reminderCron,
        String suspendCron,
        long reminderJoursAvant) {
}

package app.autoeecoleconnect.controlplane.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "provisioning_logs")
public class ProvisioningLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // provision | suspend | resume | delete
    @Column(nullable = false, length = 100)
    private String action;

    // pending | success | failed
    @Column(nullable = false, length = 50)
    private String statut;

    @Column(columnDefinition = "text")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public ProvisioningLog() {
    }

    public ProvisioningLog(Tenant tenant, String action, String statut, String detail) {
        this.tenant = tenant;
        this.action = action;
        this.statut = statut;
        this.detail = detail;
    }

    public UUID getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public String getAction() {
        return action;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

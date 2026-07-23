package app.autoeecoleconnect.models;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Créneau de disponibilité <b>récurrent hebdomadaire</b> d'un moniteur (backlog
 * #35) : « tous les lundis de 9h à 12h ». Un moniteur en a plusieurs. Alimente
 * le KPI taux d'occupation (heures travaillées / heures disponibles déclarées).
 */
@Entity
@Table(name = "disponibilites_moniteur")
public class DisponibiliteMoniteur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "moniteur_id", nullable = false)
    private Moniteur moniteur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JourSemaine jour;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "heure_fin", nullable = false)
    private LocalTime heureFin;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "auto_ecole_id", nullable = false)
    private UUID autoEcoleId;

    public UUID getId() {
        return id;
    }

    public Moniteur getMoniteur() {
        return moniteur;
    }

    public void setMoniteur(Moniteur moniteur) {
        this.moniteur = moniteur;
    }

    public JourSemaine getJour() {
        return jour;
    }

    public void setJour(JourSemaine jour) {
        this.jour = jour;
    }

    public LocalTime getHeureDebut() {
        return heureDebut;
    }

    public void setHeureDebut(LocalTime heureDebut) {
        this.heureDebut = heureDebut;
    }

    public LocalTime getHeureFin() {
        return heureFin;
    }

    public void setHeureFin(LocalTime heureFin) {
        this.heureFin = heureFin;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getAutoEcoleId() {
        return autoEcoleId;
    }

    public void setAutoEcoleId(UUID autoEcoleId) {
        this.autoEcoleId = autoEcoleId;
    }
}

package app.autoeecoleconnect.models;

import java.time.LocalDate;
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

@Entity
@Table(name = "seances")
public class Seance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moniteur_id")
    private Moniteur moniteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voiture_id")
    private Voiture voiture;

    @Column(name = "date_seance", nullable = false)
    private LocalDate dateSeance;

    @Column(name = "h_deb", nullable = false)
    private LocalTime hDeb;

    @Column(name = "h_fin", nullable = false)
    private LocalTime hFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private StatutSeance statut = StatutSeance.SCHEDULED;

    @Column(name = "validated_client", nullable = false)
    private boolean validatedClient;

    @Column(name = "validated_moniteur", nullable = false)
    private boolean validatedMoniteur;

    @Column(name = "validated_admin", nullable = false)
    private boolean validatedAdmin;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Moniteur getMoniteur() {
        return moniteur;
    }

    public void setMoniteur(Moniteur moniteur) {
        this.moniteur = moniteur;
    }

    public Voiture getVoiture() {
        return voiture;
    }

    public void setVoiture(Voiture voiture) {
        this.voiture = voiture;
    }

    public LocalDate getDateSeance() {
        return dateSeance;
    }

    public void setDateSeance(LocalDate dateSeance) {
        this.dateSeance = dateSeance;
    }

    public LocalTime getHDeb() {
        return hDeb;
    }

    public void setHDeb(LocalTime hDeb) {
        this.hDeb = hDeb;
    }

    public LocalTime getHFin() {
        return hFin;
    }

    public void setHFin(LocalTime hFin) {
        this.hFin = hFin;
    }

    public StatutSeance getStatut() {
        return statut;
    }

    public void setStatut(StatutSeance statut) {
        this.statut = statut;
    }

    public boolean isValidatedClient() {
        return validatedClient;
    }

    public void setValidatedClient(boolean validatedClient) {
        this.validatedClient = validatedClient;
    }

    public boolean isValidatedMoniteur() {
        return validatedMoniteur;
    }

    public void setValidatedMoniteur(boolean validatedMoniteur) {
        this.validatedMoniteur = validatedMoniteur;
    }

    public boolean isValidatedAdmin() {
        return validatedAdmin;
    }

    public void setValidatedAdmin(boolean validatedAdmin) {
        this.validatedAdmin = validatedAdmin;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}

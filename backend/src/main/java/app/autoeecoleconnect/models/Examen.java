package app.autoeecoleconnect.models;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * Passage d'examen (code ou conduite) d'un élève — backlog #34, version
 * « suivi d'examens » : au-delà du résultat, on retient la convocation, le
 * centre, l'examinateur et le nombre de fautes. Alimente le KPI taux de
 * réussite (docs/13-analytics.md).
 */
@Entity
@Table(name = "examens")
public class Examen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TypeExamen type;

    @Column(name = "date_examen", nullable = false)
    private LocalDate dateExamen;

    @Column(name = "date_convocation")
    private LocalDate dateConvocation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ResultatExamen resultat = ResultatExamen.PLANIFIE;

    @Column(name = "nombre_fautes")
    private Integer nombreFautes;

    @Column(name = "centre_examen", length = 255)
    private String centreExamen;

    @Column(length = 255)
    private String examinateur;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Rattachement à l'agence (docs/17), même dénormalisation volontaire que
    // reservations/seances : un simple UUID indexé, filtrable localement et
    // impossible à oublier en silence, plutôt qu'une jointure vers le client.
    @Column(name = "auto_ecole_id", nullable = false)
    private UUID autoEcoleId;

    public UUID getId() {
        return id;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public TypeExamen getType() {
        return type;
    }

    public void setType(TypeExamen type) {
        this.type = type;
    }

    public LocalDate getDateExamen() {
        return dateExamen;
    }

    public void setDateExamen(LocalDate dateExamen) {
        this.dateExamen = dateExamen;
    }

    public LocalDate getDateConvocation() {
        return dateConvocation;
    }

    public void setDateConvocation(LocalDate dateConvocation) {
        this.dateConvocation = dateConvocation;
    }

    public ResultatExamen getResultat() {
        return resultat;
    }

    public void setResultat(ResultatExamen resultat) {
        this.resultat = resultat;
    }

    public Integer getNombreFautes() {
        return nombreFautes;
    }

    public void setNombreFautes(Integer nombreFautes) {
        this.nombreFautes = nombreFautes;
    }

    public String getCentreExamen() {
        return centreExamen;
    }

    public void setCentreExamen(String centreExamen) {
        this.centreExamen = centreExamen;
    }

    public String getExaminateur() {
        return examinateur;
    }

    public void setExaminateur(String examinateur) {
        this.examinateur = examinateur;
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

    public UUID getAutoEcoleId() {
        return autoEcoleId;
    }

    public void setAutoEcoleId(UUID autoEcoleId) {
        this.autoEcoleId = autoEcoleId;
    }
}

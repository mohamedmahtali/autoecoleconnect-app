package app.autoeecoleconnect.models;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Une agence de l'organisation. Depuis la refonte du grain de tenancy
 * (docs/17), une base porte une organisation et non plus une seule école :
 * toutes les autres entités se rattachent à une {@code AutoEcole} par leur
 * colonne {@code auto_ecole_id}.
 *
 * <p>Le {@code slug} est le sous-domaine public de l'école
 * ({@code <slug>.autoecoleconnect.fr}). Son unicité est garantie
 * globalement par le control-plane ({@code SlugService}), pas seulement
 * dans cette base — deux organisations ne peuvent pas revendiquer le même.
 */
@Entity
@Table(name = "auto_ecoles")
public class AutoEcole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(columnDefinition = "text")
    private String adresse;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
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

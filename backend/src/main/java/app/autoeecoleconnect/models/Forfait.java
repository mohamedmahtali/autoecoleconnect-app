package app.autoeecoleconnect.models;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "forfaits")
public class Forfait {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nom;

    @Column(name = "nombre_heure", nullable = false)
    private Integer nombreHeure;

    @Column(nullable = false)
    private Integer validite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private UniteValidite unite;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    @Column(columnDefinition = "text")
    private String conditions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CategorieForfait categorie;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Transmission transmission;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kilometrage kilometrage;

    @Column(name = "nb_kilometre")
    private Integer nbKilometre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CarburantForfait carburant;

    @Column(nullable = false)
    private boolean active = true;

    public UUID getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public Integer getNombreHeure() {
        return nombreHeure;
    }

    public void setNombreHeure(Integer nombreHeure) {
        this.nombreHeure = nombreHeure;
    }

    public Integer getValidite() {
        return validite;
    }

    public void setValidite(Integer validite) {
        this.validite = validite;
    }

    public UniteValidite getUnite() {
        return unite;
    }

    public void setUnite(UniteValidite unite) {
        this.unite = unite;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public String getConditions() {
        return conditions;
    }

    public void setConditions(String conditions) {
        this.conditions = conditions;
    }

    public CategorieForfait getCategorie() {
        return categorie;
    }

    public void setCategorie(CategorieForfait categorie) {
        this.categorie = categorie;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public void setTransmission(Transmission transmission) {
        this.transmission = transmission;
    }

    public Kilometrage getKilometrage() {
        return kilometrage;
    }

    public void setKilometrage(Kilometrage kilometrage) {
        this.kilometrage = kilometrage;
    }

    public Integer getNbKilometre() {
        return nbKilometre;
    }

    public void setNbKilometre(Integer nbKilometre) {
        this.nbKilometre = nbKilometre;
    }

    public CarburantForfait getCarburant() {
        return carburant;
    }

    public void setCarburant(CarburantForfait carburant) {
        this.carburant = carburant;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // Rattachement à l'agence (docs/17). Simple UUID plutôt qu'un @ManyToOne :
    // le seul usage est le filtrage, une association ajouterait une jointure
    // et un chargement paresseux sans rien apporter ici.
    @Column(name = "auto_ecole_id", nullable = false)
    private UUID autoEcoleId;

    public UUID getAutoEcoleId() {
        return autoEcoleId;
    }

    public void setAutoEcoleId(UUID autoEcoleId) {
        this.autoEcoleId = autoEcoleId;
    }

}

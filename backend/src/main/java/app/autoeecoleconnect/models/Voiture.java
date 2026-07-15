package app.autoeecoleconnect.models;

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
@Table(name = "voitures")
public class Voiture {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String marque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Transmission transmission;

    @Column(name = "double_commande", nullable = false)
    private boolean doubleCommande;

    @Column(length = 50)
    private String carburant;

    @Column(length = 50)
    private String couleur;

    @Column(name = "nb_portes")
    private Integer nbPortes;

    @Column(name = "nb_passagers")
    private Integer nbPassagers;

    @Column(name = "air_conditionne", nullable = false)
    private boolean airConditionne;

    @Column(columnDefinition = "text")
    private String note;

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

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public Transmission getTransmission() {
        return transmission;
    }

    public void setTransmission(Transmission transmission) {
        this.transmission = transmission;
    }

    public boolean isDoubleCommande() {
        return doubleCommande;
    }

    public void setDoubleCommande(boolean doubleCommande) {
        this.doubleCommande = doubleCommande;
    }

    public String getCarburant() {
        return carburant;
    }

    public void setCarburant(String carburant) {
        this.carburant = carburant;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public Integer getNbPortes() {
        return nbPortes;
    }

    public void setNbPortes(Integer nbPortes) {
        this.nbPortes = nbPortes;
    }

    public Integer getNbPassagers() {
        return nbPassagers;
    }

    public void setNbPassagers(Integer nbPassagers) {
        this.nbPassagers = nbPassagers;
    }

    public boolean isAirConditionne() {
        return airConditionne;
    }

    public void setAirConditionne(boolean airConditionne) {
        this.airConditionne = airConditionne;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

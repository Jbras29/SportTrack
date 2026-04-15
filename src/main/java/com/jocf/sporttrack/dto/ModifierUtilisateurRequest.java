package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.enumeration.NiveauPratiqueSportive;
import com.jocf.sporttrack.model.Utilisateur;

import java.util.List;

/**
 * Mise à jour de profil (API JSON ou formulaire profil) — pas d'entité JPA en entrée contrôleur.
 */
public class ModifierUtilisateurRequest {

    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private String motdepasse;
    private String sexe;
    private Integer age;
    private Double poids;
    private Double taille;
    private String objectifsPersonnels;
    private NiveauPratiqueSportive niveauPratiqueSportive;
    private boolean comptePrive;
    private String photoProfil;
    private List<Long> prefSportivesIds;

    public static ModifierUtilisateurRequest fromUtilisateur(Utilisateur u) {
        ModifierUtilisateurRequest r = new ModifierUtilisateurRequest();
        r.setId(u.getId());
        r.setNom(u.getNom());
        r.setPrenom(u.getPrenom());
        r.setEmail(u.getEmail());
        r.setMotdepasse(null);
        r.setSexe(u.getSexe());
        r.setAge(u.getAge());
        r.setPoids(u.getPoids());
        r.setTaille(u.getTaille());
        r.setObjectifsPersonnels(u.getObjectifsPersonnels());
        r.setNiveauPratiqueSportive(u.getNiveauPratiqueSportive());
        r.setComptePrive(u.isComptePrive());
        r.setPhotoProfil(u.getPhotoProfil());
        r.setPrefSportivesIds(null);
        return r;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotdepasse() {
        return motdepasse;
    }

    public void setMotdepasse(String motdepasse) {
        this.motdepasse = motdepasse;
    }

    public String getSexe() {
        return sexe;
    }

    public void setSexe(String sexe) {
        this.sexe = sexe;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Double getPoids() {
        return poids;
    }

    public void setPoids(Double poids) {
        this.poids = poids;
    }

    public Double getTaille() {
        return taille;
    }

    public void setTaille(Double taille) {
        this.taille = taille;
    }

    public String getObjectifsPersonnels() {
        return objectifsPersonnels;
    }

    public void setObjectifsPersonnels(String objectifsPersonnels) {
        this.objectifsPersonnels = objectifsPersonnels;
    }

    public NiveauPratiqueSportive getNiveauPratiqueSportive() {
        return niveauPratiqueSportive;
    }

    public void setNiveauPratiqueSportive(NiveauPratiqueSportive niveauPratiqueSportive) {
        this.niveauPratiqueSportive = niveauPratiqueSportive;
    }

    public boolean isComptePrive() {
        return comptePrive;
    }

    public void setComptePrive(boolean comptePrive) {
        this.comptePrive = comptePrive;
    }

    public String getPhotoProfil() {
        return photoProfil;
    }

    public void setPhotoProfil(String photoProfil) {
        this.photoProfil = photoProfil;
    }

    public List<Long> getPrefSportivesIds() {
        return prefSportivesIds;
    }

    public void setPrefSportivesIds(List<Long> prefSportivesIds) {
        this.prefSportivesIds = prefSportivesIds;
    }
}

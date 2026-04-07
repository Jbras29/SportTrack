package com.jocf.sporttrack.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "badges")
public class Badge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private int kmNecessaire;
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
    public int getKmNecessaire() {
        return kmNecessaire;
    }
    public void setKmNecessaire(int kmNecessaire) {
        this.kmNecessaire = kmNecessaire;
    }
    public Badge(Long id, String nom, int kmNecessaire) {
        this.id = id;
        this.nom = nom;
        this.kmNecessaire = kmNecessaire;
    }
    public Badge() {
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((nom == null) ? 0 : nom.hashCode());
        result = prime * result + kmNecessaire;
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Badge other = (Badge) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (nom == null) {
            if (other.nom != null)
                return false;
        } else if (!nom.equals(other.nom))
            return false;
        if (kmNecessaire != other.kmNecessaire)
            return false;
        return true;
    }
    
    @ManyToMany(mappedBy = "badges")
    private java.util.List<Utilisateur> utilisateurs;

}

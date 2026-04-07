package com.jocf.sporttrack.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "utilisateurs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String motdepasse;

    private String sexe;
    private Integer age;
    private Double poids;
    private Double taille;
    private Integer xp;
    private Integer hp;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "utilisateur_amis",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "ami_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Utilisateur> amis = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "utilisateur_demandes_amis",
            joinColumns = @JoinColumn(name = "expediteur_id"),
            inverseJoinColumns = @JoinColumn(name = "destinataire_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Utilisateur> demandesAmisEnvoyees = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "utilisateur_pref_sportives",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "pref_sportive_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<PrefSportive> prefSportives = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Activite> activites = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "organisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Evenement> evenementsOrganises = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "participants")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Evenement> evenementsParticipes = new ArrayList<>();
}

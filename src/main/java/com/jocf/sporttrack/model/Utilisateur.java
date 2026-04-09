package com.jocf.sporttrack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /**
     * Seuils cumulatifs d’XP pour les paliers d’expérience (niveaux 2 à 6).
     * L’indice 0 vaut toujours 0 ; l’indice {@code i} est le minimum d’XP pour être au moins au niveau {@code i + 1}.
     */
    public static final int[] SEUILS_XP_NIVEAU_EXPERIENCE = {0, 100, 500, 1000, 3000, 5000};

    /**
     * Partie fixe : récompense toute séance enregistrée.
     */
    private static final double XP_ACTIVITE_BASE = 8.0;

    /** Poids de la distance (km) dans l’XP. */
    private static final double XP_COEF_DISTANCE_KM = 1.8;

    /** Poids de la durée (minutes) dans l’XP. */
    private static final double XP_COEF_DUREE_MIN = 0.35;

    /**
     * Bonus « effort complet » : récompense les sorties où distance et temps sont tous deux non triviaux
     * (produit √(km × min) : même allure longue ou courte séance intense rentrent dans la formule).
     */
    private static final double XP_COEF_SYNERGIE = 2.5;

    /** Saisie typique en mètres au-delà de ce seuil (ex. 5000 m) ; en dessous on suppose des km (ex. 10,5). */
    private static final double XP_SEUIL_DISTANCE_METRES = 400.0;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String motdepasse;

    /**
     * Chemin public servi par l'application (ex. /uploads/profiles/1-uuid.jpg), ou null pour utiliser le placeholder.
     */
    @Column(length = 512)
    private String photoProfil;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeUtilisateur typeUtilisateur = TypeUtilisateur.UTILISATEUR;

    private String sexe;
    private Integer age;
    private Double poids;
    private Double taille;
    private Integer xp;
    private Integer hp;

    @Column(length = 4000)
    private String objectifsPersonnels;

    @Enumerated(EnumType.STRING)
    private NiveauPratiqueSportive niveauPratiqueSportive;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "utilisateur_amis",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "ami_id")
    )
    @JsonIgnoreProperties("amis")
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
    @JsonIgnoreProperties({"amis", "demandesAmisEnvoyees"})
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
    @ManyToMany
    @JoinTable(
            name = "utilisateur_badges",
            joinColumns = @JoinColumn(name = "utilisateur_id"),
            inverseJoinColumns = @JoinColumn(name = "badges_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Badge> badges = new ArrayList<>();
  
    @OneToMany(mappedBy = "organisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Evenement> evenementsOrganises = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "participants")
    @JsonIgnoreProperties("participants")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Evenement> evenementsParticipes = new ArrayList<>();

    /** URL de l'image à afficher : photo utilisateur ou image par défaut sous /static. */
    public String cheminPhotoProfilAffichee() {
        if (photoProfil != null && !photoProfil.isBlank()) {
            return photoProfil;
        }
        return "/images/profile-placeholder.svg";
    }

    public int getXpEffectif() {
        return xp != null ? xp : 0;
    }

    /**
     * Interprète la distance en base pour la formule d’XP : au-delà du seuil « mètres »,
     * la valeur est traitée comme des mètres (ex. 5000 → 5 km), sinon comme des kilomètres déjà (ex. 10 → 10 km).
     */
    public static double distanceEnKmPourFormuleXp(Double distanceStockee) {
        if (distanceStockee == null || distanceStockee <= 0) {
            return 0.0;
        }
        double v = distanceStockee;
        return v > XP_SEUIL_DISTANCE_METRES ? v / 1000.0 : v;
    }

    /**
     * XP gagnée pour une activité : base + effort distance + effort durée + bonus synergie √(km×min).
     * Les paramètres attendus sont la distance déjà ramenée en km (voir {@link #distanceEnKmPourFormuleXp}) et la durée en minutes.
     *
     * @param distanceKm distance parcourue en kilomètres
     * @param dureeMinutes durée en minutes
     * @return XP entière (au moins 1 pour toute activité enregistrée)
     */
    public static int calculerXpGagnePourActivite(double distanceKm, int dureeMinutes) {
        double d = Math.max(0.0, distanceKm);
        double t = Math.max(0.0, dureeMinutes);
        double score = XP_ACTIVITE_BASE + XP_COEF_DISTANCE_KM * d + XP_COEF_DUREE_MIN * t
                + XP_COEF_SYNERGIE * Math.sqrt(d * t);
        int arrondi = (int) Math.floor(score);
        return Math.max(1, arrondi);
    }

    /** HP affichable (0–100), avec valeur par défaut à 100 si non renseigné. */
    public int getHpNormalise() {
        int h = hp != null ? hp : 100;
        return Math.max(0, Math.min(100, h));
    }

    /**
     * Palier d’expérience « jeu » (1 à 6) dérivé de l’XP, indépendant de {@link #niveauPratiqueSportive}.
     */
    public int getNiveauExperience() {
        int x = getXpEffectif();
        if (x >= SEUILS_XP_NIVEAU_EXPERIENCE[5]) {
            return 6;
        }
        if (x >= SEUILS_XP_NIVEAU_EXPERIENCE[4]) {
            return 5;
        }
        if (x >= SEUILS_XP_NIVEAU_EXPERIENCE[3]) {
            return 4;
        }
        if (x >= SEUILS_XP_NIVEAU_EXPERIENCE[2]) {
            return 3;
        }
        if (x >= SEUILS_XP_NIVEAU_EXPERIENCE[1]) {
            return 2;
        }
        return 1;
    }

    /** XP accumulés depuis le seuil du palier actuel (affichage type 250 / 400). */
    public int getXpDepuisSeuilNiveauExperience() {
        int niveau = getNiveauExperience();
        int bas = SEUILS_XP_NIVEAU_EXPERIENCE[niveau - 1];
        return Math.max(0, getXpEffectif() - bas);
    }

    /**
     * Seuil d’XP du palier suivant (borne haute de la jauge).
     * Au palier maximum, retourne le dernier seuil défini ({@link #SEUILS_XP_NIVEAU_EXPERIENCE}[5]).
     */
    public int getXpSeuilProchainNiveauExperience() {
        int niveau = getNiveauExperience();
        if (niveau >= 6) {
            return SEUILS_XP_NIVEAU_EXPERIENCE[5];
        }
        return SEUILS_XP_NIVEAU_EXPERIENCE[niveau];
    }

    /** Pourcentage de remplissage de la jauge XP jusqu’au palier suivant (100 au maximum). */
    public double getPourcentageBarreExperience() {
        int niveau = getNiveauExperience();
        if (niveau >= 6) {
            return 100.0;
        }
        int bas = SEUILS_XP_NIVEAU_EXPERIENCE[niveau - 1];
        int haut = SEUILS_XP_NIVEAU_EXPERIENCE[niveau];
        int plage = haut - bas;
        if (plage <= 0) {
            return 100.0;
        }
        return 100.0 * (getXpEffectif() - bas) / plage;
    }
}

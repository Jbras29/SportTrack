package com.jocf.sporttrack.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.jocf.sporttrack.enumeration.TypeCommentaire;
import com.jocf.sporttrack.enumeration.TypeSport;

@Entity
@Table(name = "activites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Activite {

    /** Nombre maximal de types de réaction (emojis distincts) affichés dans le fil d’actualité. */
    private static final int LIMITE_REACTIONS_AFFICHEES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sport", nullable = false)
    private TypeSport typeSport;

    private Double distance;

    private Integer temps;

    @Column(nullable = false)
    private LocalDate date;

    private String location;

    private Integer evaluation;

    
    
    /** XP attribuée à l’enregistrement de cette activité (historique / affichage). */
    private Integer xpGagne;

    private Double calories;

    private Double meteoTemperature;

    private String meteoCondition;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Builder.Default
    @OneToMany(mappedBy = "activite", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("dateCreation ASC")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Commentaire> commentaires = new ArrayList<>();

    /** Commentaires textuels uniquement, dans l’ordre chronologique de la liste. */
    public List<Commentaire> getCommentairesMessages() {
        return commentaires.stream()
                .filter(c -> c.getType() == TypeCommentaire.MESSAGE)
                .toList();
    }

    /**
     * Réactions par emoji avec prénoms / noms des auteurs (ordre chronologique des réactions).
     */
    public List<ReactionGroupee> getReactionsGroupees() {
        Map<String, List<Utilisateur>> parEmoji = new LinkedHashMap<>();
        for (Commentaire c : commentaires) {
            if (c.getType() == TypeCommentaire.REACTION && c.getAuteur() != null) {
                parEmoji.computeIfAbsent(c.getMessage(), k -> new ArrayList<>()).add(c.getAuteur());
            }
        }
        List<ReactionGroupee> resultat = new ArrayList<>();
        for (Map.Entry<String, List<Utilisateur>> e : parEmoji.entrySet()) {
            String noms = e.getValue().stream()
                    .map(u -> u.getPrenom() + " " + u.getNom())
                    .collect(Collectors.joining(", "));
            resultat.add(new ReactionGroupee(e.getKey(), e.getValue().size(), noms));
        }
        return resultat;
    }

    /**
     * Sous-ensemble des réactions groupées pour l’affichage (évite une barre trop longue).
     */
    public List<ReactionGroupee> getReactionsGroupeesAffichees() {
        List<ReactionGroupee> toutes = getReactionsGroupees();
        if (toutes.size() <= LIMITE_REACTIONS_AFFICHEES) {
            return toutes;
        }
        return new ArrayList<>(toutes.subList(0, LIMITE_REACTIONS_AFFICHEES));
    }

    /**
     * Nombre de types de réaction non affichés (au-delà de la limite d’affichage du fil).
     */
    public int getReactionsGroupeesMasqueesCount() {
        int total = getReactionsGroupees().size();
        return Math.max(0, total - LIMITE_REACTIONS_AFFICHEES);
    }

    /**
     * Indique si l’utilisateur a déjà réagi avec cet emoji sur cette activité (une entrée par auteur et par emoji).
     */
    public boolean utilisateurAEmitReactionAvecEmoji(Long utilisateurId, String emoji) {
        return getIdCommentaireReactionUtilisateur(utilisateurId, emoji) != null;
    }

    /**
     * Identifiant du commentaire de réaction pour cet utilisateur et cet emoji, s’il existe.
     */
    public Long getIdCommentaireReactionUtilisateur(Long utilisateurId, String emoji) {
        if (utilisateurId == null || emoji == null) {
            return null;
        }
        for (Commentaire c : commentaires) {
            if (c.getType() == TypeCommentaire.REACTION
                    && emoji.equals(c.getMessage())
                    && c.getAuteur() != null
                    && utilisateurId.equals(c.getAuteur().getId())) {
                return c.getId();
            }
        }
        return null;
    }

    /**
     * Réactions agrégées par emoji (chaîne stockée en {@link Commentaire#getMessage()}),
     * ordre d’apparition conservé.
     */
    public Map<String, Long> getReactionsParEmoji() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ReactionGroupee g : getReactionsGroupees()) {
            counts.put(g.emoji(), g.nombre());
        }
        return counts;
    }

    public static int getLimiteReactionsAffichees() {
        return LIMITE_REACTIONS_AFFICHEES;
    }
    
    @ManyToMany
    @JoinTable(
        name = "activite_utilisateurs_invites",
        joinColumns = @JoinColumn(name = "activite_id"),
        inverseJoinColumns = @JoinColumn(name = "utilisateur_id")
    )
@Builder.Default
private List<Utilisateur> invites = new ArrayList<>();
}

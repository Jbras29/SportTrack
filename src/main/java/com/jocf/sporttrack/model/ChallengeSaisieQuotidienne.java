package com.jocf.sporttrack.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Indique pour un jour donné si le participant a déclaré avoir réalisé (ou non) le défi quotidien.
 * Absence de ligne pour un jour = pas encore de réponse (rappel possible).
 */
@Entity
@Table(
        name = "challenge_saisies_quotidiennes",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_challenge_user_jour",
                columnNames = {"challenge_id", "utilisateur_id", "jour"}
        )
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeSaisieQuotidienne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "challenge_id", nullable = false)
    private Challenge challenge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private LocalDate jour;

    /** {@code true} = objectif atteint, {@code false} = déclaré comme non fait. */
    @Column(nullable = false)
    private boolean realise;
}

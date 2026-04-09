package com.jocf.sporttrack.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "commentaires")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"activite"})
public class Commentaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeCommentaire type;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "auteur_id", nullable = false)
    @JsonIgnoreProperties({
            "motdepasse", "activites", "amis", "demandesAmisEnvoyees",
            "prefSportives", "badges", "evenementsOrganises", "evenementsParticipes",
            "messagesEnvoyes", "messagesRecus"
    })
    private Utilisateur auteur;

    @ManyToOne
    @JoinColumn(name = "activite_id", nullable = false)
    private Activite activite;
}

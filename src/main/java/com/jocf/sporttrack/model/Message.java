package com.jocf.sporttrack.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private Utilisateur expediteur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Column(nullable = false, length = 2000)
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi;

    /**
     * Accusé de réception côté destinataire : {@code false} tant que le message n’a pas été consulté
     * (liste + conversation ouverte).
     */
    @Builder.Default
    @Column(nullable = false)
    private boolean lu = false;

    /** Instant du passage à lu (null si non lu). */
    private LocalDateTime dateLu;

    /** Marque comme lu et enregistre la date d’accusé de réception (idempotent sur {@code dateLu}). */
    public void marquerCommeLu() {
        if (this.lu) {
            return;
        }
        this.lu = true;
        this.dateLu = LocalDateTime.now();
    }
}
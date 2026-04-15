package com.jocf.sporttrack.dto;

import java.time.LocalDateTime;

import com.jocf.sporttrack.enumeration.NotificationType;

/**
 * Une entrée du fil de notifications, triée par {@link #dateTri} (plus récent en premier).
 */
public record NotificationItem(
        NotificationType type,
        String titre,
        String detail,
        LocalDateTime dateTri,
        String lien,
        Long referenceId,
        /** URL affichable pour l’avatar (réactions / réponses uniquement), sinon {@code null}. */
        String photoProfilUrl,
        /** {@code true} si la notification est plus récente que la dernière consultation de la page. */
        boolean nonLue
) {
}

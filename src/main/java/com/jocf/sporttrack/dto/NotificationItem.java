package com.jocf.sporttrack.dto;

import com.jocf.sporttrack.model.NotificationType;

import java.time.LocalDateTime;

/**
 * Une entrée du fil de notifications, triée par {@link #dateTri} (plus récent en premier).
 */
public record NotificationItem(
        NotificationType type,
        String titre,
        String detail,
        LocalDateTime dateTri,
        String lien,
        Long referenceId
) {
}

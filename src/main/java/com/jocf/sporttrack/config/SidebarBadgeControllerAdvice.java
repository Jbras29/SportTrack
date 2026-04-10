package com.jocf.sporttrack.config;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.MessageService;
import com.jocf.sporttrack.service.NotificationService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Compteurs pour la barre latérale (notifications et messages non lus).
 */
@ControllerAdvice
public class SidebarBadgeControllerAdvice {

    private final NotificationService notificationService;
    private final MessageService messageService;
    private final UtilisateurService utilisateurService;

    public SidebarBadgeControllerAdvice(
            NotificationService notificationService,
            MessageService messageService,
            UtilisateurService utilisateurService) {
        this.notificationService = notificationService;
        this.messageService = messageService;
        this.utilisateurService = utilisateurService;
    }

    @ModelAttribute
    public void ajouterBadgesSidebar(HttpSession session, Model model) {
        long badgeNotifications = 0L;
        long badgeMessages = 0L;
        Long id = (Long) session.getAttribute(SessionKeys.UTILISATEUR_ID);
        if (id != null) {
            Optional<Utilisateur> u = utilisateurService.trouverParId(id);
            if (u.isPresent()) {
                badgeNotifications = notificationService.compterNotificationsNonLues(id);
                badgeMessages = messageService.compterMessagesNonLus(u.get());
            }
        }
        model.addAttribute("badgeNotificationsNonLues", badgeNotifications);
        model.addAttribute("badgeMessagesNonLus", badgeMessages);
    }
}

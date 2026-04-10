package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.NotificationService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationController {

    private final NotificationService notificationService;
    private final UtilisateurService utilisateurService;

    public NotificationController(NotificationService notificationService, UtilisateurService utilisateurService) {
        this.notificationService = notificationService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/notifications")
    public String notifications(HttpSession session, Model model) {
        SessionUtilisateur sessionUser = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (sessionUser == null) {
            return "redirect:/login";
        }
        Utilisateur user = utilisateurService.trouverParId(sessionUser.id())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + sessionUser.id()));
        model.addAttribute("user", user);
        LocalDateTime derniereVue = user.getDerniereConsultationNotifications();
        model.addAttribute("notifications", notificationService.listerPourUtilisateur(sessionUser.id(), derniereVue));
        utilisateurService.enregistrerDerniereConsultationNotifications(sessionUser.id());
        return "notifications/list";
    }
}

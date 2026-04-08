package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UtilisateurService utilisateurService;

    public DashboardController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Si l'utilisateur n'est pas authentifié, rediriger vers la page de login
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/Utilisateur/";
        }

        String email = authentication.getName();
        try {
            Utilisateur user = utilisateurService.trouverParEmail(email);

            model.addAttribute("user", user);
            model.addAttribute("level", user.getNiveauExperience());
            model.addAttribute("avatarId", user.getId() % 50);
            
            return "dashboard";
        } catch (Exception e) {
            return "redirect:/Utilisateur/";
        }
    }
}

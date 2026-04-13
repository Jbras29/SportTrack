package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerCompteRequest;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {

    private final UtilisateurService utilisateurService;

    public LoginController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "/account/register";
    }

    @PostMapping("/utilisateurs/create")
    public String creerCompte(@ModelAttribute CreerCompteRequest form, RedirectAttributes redirectAttributes) {
        try {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setNom(form.getNom());
            utilisateur.setPrenom(form.getPrenom());
            utilisateur.setEmail(form.getEmail());
            utilisateur.setMotdepasse(form.getMotdepasse());
            String sexe = form.getSexe();
            utilisateur.setSexe(sexe != null && !sexe.isBlank() ? sexe : null);
            utilisateur.setAge(form.getAge());
            utilisateur.setPoids(form.getPoids());
            utilisateur.setTaille(form.getTaille());

            utilisateurService.creerUtilisateur(utilisateur);
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/register";
        }
    }
}
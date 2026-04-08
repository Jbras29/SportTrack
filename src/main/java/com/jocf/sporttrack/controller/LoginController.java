package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        return "/create/register";
    }

    @PostMapping("/utilisateurs/create")
    public String creerCompte(
            @RequestParam String nom,
            @RequestParam String prenom,
            @RequestParam String email,
            @RequestParam("motdepasse") String motDePasse,
            @RequestParam(required = false) String sexe,
            @RequestParam(required = false) Integer age,
            @RequestParam(required = false) Double poids,
            @RequestParam(required = false) Double taille,
            RedirectAttributes redirectAttributes) {
        try {
            Utilisateur utilisateur = new Utilisateur();
            utilisateur.setNom(nom);
            utilisateur.setPrenom(prenom);
            utilisateur.setEmail(email);
            utilisateur.setMotdepasse(motDePasse);
            utilisateur.setSexe(sexe != null && !sexe.isBlank() ? sexe : null);
            utilisateur.setAge(age);
            utilisateur.setPoids(poids);
            utilisateur.setTaille(taille);

            utilisateurService.creerUtilisateur(utilisateur);
            return "redirect:/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Erreur : " + e.getMessage());
            return "redirect:/register";
        }
    }
}
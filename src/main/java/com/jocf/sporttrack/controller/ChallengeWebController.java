package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/challenges")
public class ChallengeWebController {

    private final ChallengeService challengeService;

    public ChallengeWebController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    @GetMapping("/creer")
    public String afficherFormulaireCreation(Model model, HttpSession session) {
        Utilisateur user = (Utilisateur) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }
        return "challenges/creer"; 
    }

    @PostMapping("/creer")
    public String traiterCreationChallenge(
            @RequestParam String nom,
            @RequestParam java.sql.Date dateDebut,
            @RequestParam java.sql.Date dateFin,
            HttpSession session,
            Model model) {

        Utilisateur user = (Utilisateur) session.getAttribute("user");
        if (user == null) {
            return "redirect:/login";
        }

        try {
            Challenge challenge = Challenge.builder()
                .nom(nom)
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .build();
            
            challengeService.creerChallenge(challenge, user.getId());
            return "redirect:/challenges"; 
        } catch (IllegalArgumentException e) {
            model.addAttribute("erreur", e.getMessage());
            return "challenges/creer";
        }
    }

    @GetMapping
    public String listeDesDefis(Model model) {
    model.addAttribute("challenges", challengeService.recupererTousLesChallenges());
    return "challenges/liste"; 
}
}
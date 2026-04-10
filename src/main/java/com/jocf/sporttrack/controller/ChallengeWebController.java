package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
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
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) return "redirect:/login";
        model.addAttribute("navRequestPath", "/challenges");
        return "challenges/creer-challenge";
    }

    @PostMapping("/creer")
    public String traiterCreationChallenge(
            @RequestParam String nom,
            @RequestParam java.sql.Date dateDebut,
            @RequestParam java.sql.Date dateFin,
            HttpSession session,
            Model model) {

        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) return "redirect:/login";

        try {
            Challenge challenge = Challenge.builder()
                    .nom(nom)
                    .dateDebut(dateDebut)
                    .dateFin(dateFin)
                    .build();
            challengeService.creerChallenge(challenge, user.id());
            return "redirect:/challenges";
        } catch (IllegalArgumentException e) {
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("navRequestPath", "/challenges");
            return "challenges/creer-challenge";
        }
    }

    @GetMapping
    public String listeDesDefis(Model model) {
        model.addAttribute("challenges", challengeService.recupererTousLesChallenges());
        model.addAttribute("navRequestPath", "/challenges");
        return "challenges/liste";
    }

    @GetMapping("/{id}")
public String detailChallenge(@PathVariable Long id, Model model, HttpSession session) {
    SessionUtilisateur sessionUser = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
    if (sessionUser == null) return "redirect:/login";

    Challenge challenge = challengeService.trouverParId(id)
            .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + id));

    boolean estParticipant = challenge.getParticipants()
            .stream()
            .anyMatch(p -> p.getId().equals(sessionUser.id()));

    model.addAttribute("challenge", challenge);
    model.addAttribute("classement", challengeService.getClassement(id));
    model.addAttribute("estParticipant", estParticipant);
    model.addAttribute("sessionUser", sessionUser); 
    model.addAttribute("navRequestPath", "/challenges");
    return "challenges/detail";
}

    @PostMapping("/{id}/rejoindre")
    public String rejoindreChallenge(@PathVariable Long id, HttpSession session) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) return "redirect:/login";

        try {
            challengeService.rejoindreChallenge(id, user.id());
        } catch (IllegalArgumentException e) {
            // challenge terminé ou introuvable, on redirige quand même
        }
        return "redirect:/challenges/" + id;
    }
}
package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/challenges")
public class ChallengeWebController {

    private final ChallengeService challengeService;
    private final UtilisateurService utilisateurService;

    public ChallengeWebController(ChallengeService challengeService, UtilisateurService utilisateurService) {
        this.challengeService = challengeService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/creer")
    public String afficherFormulaireCreation(Model model, HttpSession session) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) return "redirect:/login";
        utilisateurService.trouverParId(user.id()).ifPresent(u -> {
            model.addAttribute("sessionUserHp", u.getHpNormalise());
        });
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
            utilisateurService.trouverParId(user.id()).ifPresent(u -> {
                model.addAttribute("sessionUserHp", u.getHpNormalise());
            });
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute("navRequestPath", "/challenges");
            return "challenges/creer-challenge";
        }
    }

    @GetMapping
public String listeDesDefis(Model model, HttpSession session) {
    SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
    model.addAttribute("challenges", challengeService.recupererTousLesChallenges());
    model.addAttribute("sessionUser", user);
    model.addAttribute("navRequestPath", "/challenges");
    return "challenges/liste";
}

    @GetMapping("/{id}")
public String detailChallenge(@PathVariable Long id, Model model, HttpSession session) {
    SessionUtilisateur sessionUser = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
    if (sessionUser == null) return "redirect:/login";

    utilisateurService.trouverParId(sessionUser.id()).ifPresent(u -> {
        model.addAttribute("sessionUserHp", u.getHpNormalise());
    });

    Challenge challenge = challengeService.trouverParId(id)
            .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + id));

    boolean estParticipant = challenge.getParticipants()
            .stream()
            .anyMatch(p -> p.getId().equals(sessionUser.id()));

    boolean estOrganisateur = challenge.getOrganisateur() != null
    && challenge.getOrganisateur().getId().equals(sessionUser.id());

    Boolean reponseDuJour = challengeService.recupererReponseDuJour(id, sessionUser.id(), LocalDate.now());

    model.addAttribute("challenge", challenge);
    model.addAttribute("classement", challengeService.getClassement(id));
    model.addAttribute("estParticipant", estParticipant);
    model.addAttribute("sessionUser", sessionUser);
    model.addAttribute("reponseDuJour", reponseDuJour);
    model.addAttribute("aDejaReponduAujourdhui", reponseDuJour != null);
    model.addAttribute("navRequestPath", "/challenges");

    return "challenges/detail";
}

    @PostMapping("/{id}/rejoindre")
    public String rejoindreChallenge(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        // 1. Vérifier si l'utilisateur est connecté via la session
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) return "redirect:/login";


        Optional<Utilisateur> uOpt = utilisateurService.trouverParId(user.id());


        if (uOpt.isPresent() && uOpt.get().getHpNormalise() <= 0) {

            redirectAttributes.addFlashAttribute("errorHp", "Action bloquée : Votre barre de vie est à 0 !");
            return "redirect:/challenges/" + id;
        }

        try {
            // 4. Si les HP sont OK, on procède à l'inscription
            challengeService.rejoindreChallenge(id, user.id());
        } catch (IllegalArgumentException e) {
            // Challenge terminé ou introuvable
        }

        return "redirect:/challenges/" + id;
    }

    @PostMapping("/{id}/saisie-quotidienne")
    public String saisieQuotidienne(
            @PathVariable Long id,
            @RequestParam boolean realise,
            HttpSession session) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            challengeService.enregistrerSaisieQuotidienne(id, user.id(), LocalDate.now(), realise);
        } catch (IllegalArgumentException ignored) {
            // challenge introuvable ou pas participant : on redirige sans erreur bloquante
        }
        return "redirect:/challenges/" + id;
    }

    @PostMapping("/{id}/supprimer")
public String supprimerChallenge(@PathVariable Long id, HttpSession session) {
    SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
    if (user == null) {
        return "redirect:/login";
    }

    try {
        challengeService.supprimerChallengeSiOrganisateur(id, user.id());
    } catch (IllegalArgumentException e) {
        return "redirect:/challenges/" + id;
    }

    return "redirect:/challenges";
}
}
package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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

    Challenge challenge = challengeService.trouverParId(id)
            .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + id));

    boolean estParticipant = challenge.getParticipants()
            .stream()
            .anyMatch(p -> p.getId().equals(sessionUser.id()));

    boolean estOrganisateur = challenge.getOrganisateur() != null
    && challenge.getOrganisateur().getId().equals(sessionUser.id());

    Boolean reponseDuJour = challengeService.recupererReponseDuJour(id, sessionUser.id(), LocalDate.now());

    List<LigneClassementChallenge> classement = challengeService.getClassement(id);
    Map<Long, Boolean> afficherNomClassement = classement.stream()
            .collect(Collectors.toMap(
                    l -> l.getUtilisateur().getId(),
                    l -> utilisateurService.peutAfficherIdentiteVers(l.getUtilisateur(), sessionUser.id()),
                    (a, b) -> a));
    boolean afficherNomOrganisateurChallenge = challenge.getOrganisateur() != null
            && utilisateurService.peutAfficherIdentiteVers(challenge.getOrganisateur(), sessionUser.id());

    model.addAttribute("challenge", challenge);
    model.addAttribute("classement", classement);
    model.addAttribute("afficherNomClassement", afficherNomClassement);
    model.addAttribute("afficherNomOrganisateurChallenge", afficherNomOrganisateurChallenge);
    model.addAttribute("estParticipant", estParticipant);
    model.addAttribute("sessionUser", sessionUser);
    model.addAttribute("reponseDuJour", reponseDuJour);
    model.addAttribute("aDejaReponduAujourdhui", reponseDuJour != null);
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
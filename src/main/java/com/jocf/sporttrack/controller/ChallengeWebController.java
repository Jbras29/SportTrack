package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerChallengeRequest;
import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/challenges")
public class ChallengeWebController {

    private static final String ATTR_SESSION_USER_HP = "sessionUserHp";
    private static final String ATTR_NAV_REQUEST_PATH = "navRequestPath";
    private static final String PATH_CHALLENGES = "/challenges";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_CHALLENGE_ID = "redirect:/challenges/";

    private final ChallengeService challengeService;
    private final UtilisateurService utilisateurService;

    public ChallengeWebController(ChallengeService challengeService, UtilisateurService utilisateurService) {
        this.challengeService = challengeService;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping("/creer")
    public String afficherFormulaireCreation(Model model, HttpSession session) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) {
            return REDIRECT_LOGIN;
        }
        utilisateurService.trouverParId(user.id())
                .ifPresent(u -> model.addAttribute(ATTR_SESSION_USER_HP, u.getHpNormalise()));
        model.addAttribute(ATTR_NAV_REQUEST_PATH, PATH_CHALLENGES);
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
        if (user == null) {
            return REDIRECT_LOGIN;
        }

        try {
            CreerChallengeRequest req = new CreerChallengeRequest(
                    nom,
                    dateDebut.toLocalDate(),
                    dateFin.toLocalDate());
            challengeService.creerChallenge(req, user.id());
            return "redirect:" + PATH_CHALLENGES;
        } catch (IllegalArgumentException e) {
            utilisateurService.trouverParId(user.id())
                    .ifPresent(u -> model.addAttribute(ATTR_SESSION_USER_HP, u.getHpNormalise()));
            model.addAttribute("erreur", e.getMessage());
            model.addAttribute(ATTR_NAV_REQUEST_PATH, PATH_CHALLENGES);
            return "challenges/creer-challenge";
        }
    }

    @GetMapping
    public String listeDesDefis(Model model, HttpSession session) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        model.addAttribute("challenges", challengeService.recupererTousLesChallenges());
        model.addAttribute("sessionUser", user);
        model.addAttribute(ATTR_NAV_REQUEST_PATH, PATH_CHALLENGES);
        return "challenges/liste";
    }

    @GetMapping("/{id}")
    public String detailChallenge(@PathVariable Long id, Model model, HttpSession session) {
        SessionUtilisateur sessionUser = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (sessionUser == null) {
            return REDIRECT_LOGIN;
        }

        utilisateurService.trouverParId(sessionUser.id())
                .ifPresent(u -> model.addAttribute(ATTR_SESSION_USER_HP, u.getHpNormalise()));

        Challenge challenge = challengeService.trouverParId(id)
                .orElseThrow(() -> new IllegalArgumentException("Challenge introuvable : " + id));

        boolean estParticipant = challenge.getParticipants()
                .stream()
                .anyMatch(p -> p.getId().equals(sessionUser.id()));

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
        model.addAttribute(ATTR_NAV_REQUEST_PATH, PATH_CHALLENGES);

        return "challenges/detail";
    }

    @PostMapping("/{id}/rejoindre")
    public String rejoindreChallenge(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) {
            return REDIRECT_LOGIN;
        }

        Optional<Utilisateur> uOpt = utilisateurService.trouverParId(user.id());

        if (uOpt.isPresent() && uOpt.get().getHpNormalise() <= 0) {
            redirectAttributes.addFlashAttribute("errorHp", "Action bloquée : Votre barre de vie est à 0 !");
            return REDIRECT_CHALLENGE_ID + id;
        }

        try {
            challengeService.rejoindreChallenge(id, user.id());
        } catch (IllegalArgumentException e) {
            // Challenge terminé ou introuvable
        }

        return REDIRECT_CHALLENGE_ID + id;
    }

    @PostMapping("/{id}/saisie-quotidienne")
    public String saisieQuotidienne(
            @PathVariable Long id,
            @RequestParam boolean realise,
            HttpSession session) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) {
            return REDIRECT_LOGIN;
        }
        try {
            challengeService.enregistrerSaisieQuotidienne(id, user.id(), LocalDate.now(), realise);
        } catch (IllegalArgumentException ignored) {
            // challenge introuvable ou pas participant : on redirige sans erreur bloquante
        }
        return REDIRECT_CHALLENGE_ID + id;
    }

    @PostMapping("/{id}/supprimer")
    public String supprimerChallenge(@PathVariable Long id, HttpSession session) {
        SessionUtilisateur user = (SessionUtilisateur) session.getAttribute(SessionKeys.UTILISATEUR);
        if (user == null) {
            return REDIRECT_LOGIN;
        }

        try {
            challengeService.supprimerChallengeSiOrganisateur(id, user.id());
        } catch (IllegalArgumentException e) {
            return REDIRECT_CHALLENGE_ID + id;
        }

        return "redirect:" + PATH_CHALLENGES;
    }
}

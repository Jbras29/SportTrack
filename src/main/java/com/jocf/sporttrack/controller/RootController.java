package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class RootController {

    private final UtilisateurService utilisateurService;
    private final ActiviteService activiteService;
    private final ChallengeRepository challengeRepository;

    public RootController(
            UtilisateurService utilisateurService,
            ActiviteService activiteService,
            ChallengeRepository challengeRepository) {
        this.utilisateurService = utilisateurService;
        this.activiteService = activiteService;
        this.challengeRepository = challengeRepository;
    }

    @GetMapping("/")
    public String racine(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/home";
        }
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String home(Model model, Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }

        String email = authentication.getName();
        Utilisateur user = utilisateurService.trouverParEmailAvecAmis(email);

        LocalDate aujourdhui = LocalDate.now();
        List<Challenge> defis = challengeRepository.findByParticipants_IdOrderByDateFinAsc(user.getId())
                .stream()
                .filter(c -> c.getDateFin() == null || !c.getDateFin().toLocalDate().isBefore(aujourdhui))
                .toList();

        model.addAttribute("user", user);
        model.addAttribute("level", user.getNiveauExperience());
        model.addAttribute("xpBarPercent", user.getPourcentageBarreExperience());
        model.addAttribute("xpDansNiveau", user.getXpDepuisSeuilNiveauExperience());
        model.addAttribute("xpMaxNiveau", user.getXpSeuilProchainNiveauExperience());
        model.addAttribute("hpBarPercent", (double) user.getHpNormalise());
        model.addAttribute("hp", user.getHpNormalise());
        model.addAttribute("activitesAmis", activiteService.recupererActivitesDesAmis(user));
        model.addAttribute("defisEnCours", defis);
        return "home";
    }

    @GetMapping("/evenements/create")
    public String creerEvenementPage(
            Authentication authentication,
            Model model) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        Utilisateur user = utilisateurService.trouverParEmail(email);
        model.addAttribute("user", user);
        return "evenement/creer-evenement";
    }
}

package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private final UtilisateurService utilisateurService;
    private final ActiviteRepository activiteRepository;
    private final ChallengeRepository challengeRepository;

    public DashboardController(
            UtilisateurService utilisateurService,
            ActiviteRepository activiteRepository,
            ChallengeRepository challengeRepository) {
        this.utilisateurService = utilisateurService;
        this.activiteRepository = activiteRepository;
        this.challengeRepository = challengeRepository;
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
            model.addAttribute("xpBarPercent", user.getPourcentageBarreExperience());
            model.addAttribute("xpDansNiveau", user.getXpDepuisSeuilNiveauExperience());
            model.addAttribute("xpMaxNiveau", user.getXpSeuilProchainNiveauExperience());
            model.addAttribute("hpBarPercent", (double) user.getHpNormalise());

            double totalKm = activiteRepository.sumDistanceByUtilisateur(user);
            Integer totalMinutes = activiteRepository.sumTempsMinutesByUtilisateur(user);
            int minutesEffectif = totalMinutes != null ? totalMinutes : 0;
            long nbActivites = activiteRepository.countByUtilisateur(user);

            model.addAttribute("totalDistanceKm", totalKm);
            model.addAttribute("totalTempsMinutes", minutesEffectif);
            model.addAttribute("nombreActivites", nbActivites);
            model.addAttribute("kcalEstime", Math.round(minutesEffectif * 5.5));

            LocalDate aujourdhui = LocalDate.now();
            List<Challenge> defis = challengeRepository.findByParticipants_IdOrderByDateFinAsc(user.getId())
                    .stream()
                    .filter(c -> c.getDateFin() == null || !c.getDateFin().toLocalDate().isBefore(aujourdhui))
                    .toList();
            model.addAttribute("defisEnCours", defis);

            model.addAttribute("dernieresActivites", activiteRepository.findTop5ByUtilisateurOrderByDateDesc(user));

            LocalDate lundi = aujourdhui.with(DayOfWeek.MONDAY);
            LocalDate dimanche = lundi.plusDays(6);
            List<Activite> activitesSemaine =
                    activiteRepository.findByUtilisateurAndDateBetween(user, lundi, dimanche);
            int[] minutesParJour = new int[7];
            for (Activite a : activitesSemaine) {
                if (a.getDate() != null && a.getTemps() != null && a.getTemps() > 0) {
                    int index = a.getDate().getDayOfWeek().getValue() - 1;
                    minutesParJour[index] += a.getTemps();
                }
            }
            String[] labelsJours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
            List<Map<String, Object>> weekPoints = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                Map<String, Object> point = new LinkedHashMap<>();
                point.put("label", labelsJours[i]);
                point.put("minutes", minutesParJour[i]);
                weekPoints.add(point);
            }
            model.addAttribute("semaineActivite", weekPoints);
            
            return "dashboard";
        } catch (Exception e) {
            return "redirect:/Utilisateur/";
        }
    }
}

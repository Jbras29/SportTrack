package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.NiveauPratiqueSportive;
import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.PrefSportiveService;
import com.jocf.sporttrack.service.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ProfileController {

    private final UtilisateurService utilisateurService;
    private final PrefSportiveService prefSportiveService;

    public ProfileController(UtilisateurService utilisateurService, PrefSportiveService prefSportiveService) {
        this.utilisateurService = utilisateurService;
        this.prefSportiveService = prefSportiveService;
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(@RequestParam(required = false) Long id, HttpSession session, Model model) {
        Long idEffectif = id != null ? id : (Long) session.getAttribute("utilisateurId");
        if (idEffectif == null) {
            return "redirect:/login";
        }
        Utilisateur utilisateur = utilisateurService.trouverParId(idEffectif)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + idEffectif));
        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("toutesPrefSportives", prefSportiveService.recupererToutesLesPrefSportives());
        model.addAttribute("niveauxPratique", NiveauPratiqueSportive.values());
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String editProfileSubmit(
            @ModelAttribute Utilisateur utilisateurDetails,
            @RequestParam(required = false) List<Long> prefSportiveIds) {
        if (utilisateurDetails.getMotdepasse() != null && utilisateurDetails.getMotdepasse().isBlank()) {
            utilisateurDetails.setMotdepasse(null);
        }
        List<PrefSportive> preferences = new ArrayList<>();
        if (prefSportiveIds != null) {
            for (Long pid : prefSportiveIds) {
                preferences.add(PrefSportive.builder().id(pid).build());
            }
        }
        utilisateurDetails.setPrefSportives(preferences);
        utilisateurService.modifierUtilisateur(utilisateurDetails.getId(), utilisateurDetails);
        return "redirect:/profile/edit?id=" + utilisateurDetails.getId();
    }
}

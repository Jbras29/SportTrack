package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.NiveauPratiqueSportive;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.PhotoProfilStorageService;
import com.jocf.sporttrack.service.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Comparator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UtilisateurService utilisateurService;
    private final PhotoProfilStorageService photoProfilStorageService;
    private final ActiviteService activiteService;

    public ProfileController(
            UtilisateurService utilisateurService,
            PhotoProfilStorageService photoProfilStorageService,
            ActiviteService activiteService) {
        this.utilisateurService = utilisateurService;
        this.photoProfilStorageService = photoProfilStorageService;
        this.activiteService = activiteService;
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(@RequestParam(required = false) Long id, HttpSession session, Model model) {
        Long idSession = (Long) session.getAttribute("utilisateurId");
        if (idSession == null) {
            return "redirect:/login";
        }
        Long idEffectif = id != null ? id : idSession;
        if (!idEffectif.equals(idSession)) {
            return "redirect:/profile/edit";
        }
        Utilisateur utilisateur = utilisateurService.trouverParId(idEffectif)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + idEffectif));
        utilisateur.getPrefSportives().sort(Comparator.comparing(
                pref -> pref.getNom() != null ? pref.getNom().toLowerCase() : ""));
        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("niveauxPratique", NiveauPratiqueSportive.values());
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String editProfileSubmit(@ModelAttribute Utilisateur utilisateurDetails, HttpSession session) {
        Long idSession = (Long) session.getAttribute("utilisateurId");
        if (idSession == null || !utilisateurDetails.getId().equals(idSession)) {
            return "redirect:/login";
        }
        if (utilisateurDetails.getMotdepasse() != null && utilisateurDetails.getMotdepasse().isBlank()) {
            utilisateurDetails.setMotdepasse(null);
        }
        utilisateurDetails.setPrefSportives(null);
        utilisateurService.modifierUtilisateur(utilisateurDetails.getId(), utilisateurDetails);
        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/preferences")
    public String ajouterPreferenceSportive(
            @RequestParam(required = false) Long id,
            @RequestParam String nomPreference,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long idSession = (Long) session.getAttribute("utilisateurId");
        if (idSession == null) {
            return "redirect:/login";
        }
        if (id != null && !id.equals(idSession)) {
            return "redirect:/profile/edit";
        }

        try {
            utilisateurService.ajouterPrefSportive(idSession, nomPreference);
            redirectAttributes.addFlashAttribute("preferenceMessage", "Preference sportive ajoutee.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("preferenceErreur", exception.getMessage());
        }

        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/preferences/{prefSportiveId}/delete")
    public String supprimerPreferenceSportive(
            @PathVariable Long prefSportiveId,
            @RequestParam(required = false) Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long idSession = (Long) session.getAttribute("utilisateurId");
        if (idSession == null) {
            return "redirect:/login";
        }
        if (id != null && !id.equals(idSession)) {
            return "redirect:/profile/edit";
        }

        try {
            utilisateurService.supprimerPrefSportive(idSession, prefSportiveId);
            redirectAttributes.addFlashAttribute("preferenceMessage", "Preference sportive supprimee.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("preferenceErreur", exception.getMessage());
        }

        return "redirect:/profile/edit";
    }

    @PostMapping("/profile/photo")
    public String televerserPhotoProfil(
            @RequestParam("file") MultipartFile fichier,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long idSession = (Long) session.getAttribute("utilisateurId");
        if (idSession == null) {
            return "redirect:/login";
        }
        try {
            String chemin = photoProfilStorageService.enregistrerPhotoProfil(fichier, idSession);
            utilisateurService.modifierPhotoProfil(idSession, chemin);
            redirectAttributes.addFlashAttribute("photoMessage", "Photo de profil mise à jour.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("photoErreur", e.getMessage());
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("photoErreur", "Impossible d'enregistrer la photo.");
        }
        return "redirect:/profile/edit?id=" + idSession;
    }

    //1 profile/ et profile/edit

    //Dans profile
    //Toutes les activités de l'utilisateur,son propre info, ces defis, ces defauts, ces amis,les évènements auxquels il participe

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        Long idSession = (Long) session.getAttribute("utilisateurId");
        if (idSession == null) {
            return "redirect:/login";
        }
        Utilisateur utilisateur = utilisateurService.trouverParId(idSession)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + idSession));
        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("user", utilisateur);
        model.addAttribute("activites", activiteService.recupererActivitesPourProfil(utilisateur));
        //model.addAttribute("defis", utilisateur.getDefis());
        //model.addAttribute("defauts", utilisateur.getDefauts());
        model.addAttribute("amis", utilisateur.getAmis());
        model.addAttribute("evenementsOrganises", utilisateur.getEvenementsOrganises());
        model.addAttribute("evenementsParticipe", utilisateur.getEvenementsParticipes());
        return "profile/view";
    }

   @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable Long id, HttpSession session, Model model) {
        Long idSession = (Long) session.getAttribute("utilisateurId");
        if (idSession == null) {
            return "redirect:/login";
        }
        Utilisateur utilisateur = utilisateurService.trouverParId(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + id));
        Utilisateur connecte = utilisateurService.findByIdWithAmis(idSession);

        connecte.getAmis().forEach(ami ->
            System.out.println("DEBUG ami.id = " + ami.getId() + " | id visité = " + id)
        );

        boolean estAmi = connecte.getAmis().stream()
                .anyMatch(ami -> ami.getId().longValue() == id.longValue());

        System.out.println("DEBUG estAmi = " + estAmi + " | amis = " + connecte.getAmis().size());

        model.addAttribute("utilisateur", utilisateur);
        model.addAttribute("user", connecte);
        model.addAttribute("estAmi", estAmi);
        model.addAttribute("activites", activiteService.recupererActivitesPourProfil(utilisateur));
        model.addAttribute("amis", utilisateur.getAmis());
        model.addAttribute("evenementsOrganises", utilisateur.getEvenementsOrganises());
        model.addAttribute("evenementsParticipe", utilisateur.getEvenementsParticipes());
        return "profile/view";
    }
}

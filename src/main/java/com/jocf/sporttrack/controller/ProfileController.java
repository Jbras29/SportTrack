package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.ModifierUtilisateurRequest;
import com.jocf.sporttrack.model.NiveauPratiqueSportive;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.PhotoProfilStorageService;
import com.jocf.sporttrack.service.UtilisateurService;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.jocf.sporttrack.repository.UtilisateurRepository;
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

    private static final String SESSION_UTILISATEUR_ID = "utilisateurId";
    private static final String ATTR_UTILISATEUR = "utilisateur";
    private static final String ATTR_ACTIVITES = "activites";
    private static final String ATTR_EVENEMENTS_ORGANISES = "evenementsOrganises";
    private static final String ATTR_EVENEMENTS_PARTICIPE = "evenementsParticipe";
    private static final String REDIRECT_LOGIN = "redirect:/login";
    private static final String REDIRECT_PROFILE_EDIT = "redirect:/profile/edit";
    private static final String MSG_UTILISATEUR_INTROUVABLE = "Utilisateur introuvable : ";

    private final UtilisateurService utilisateurService;
    private final PhotoProfilStorageService photoProfilStorageService;
    private final ActiviteService activiteService;
    private final UtilisateurRepository utilisateurRepository;

    public ProfileController(
            UtilisateurService utilisateurService,
            PhotoProfilStorageService photoProfilStorageService,
            ActiviteService activiteService,
            UtilisateurRepository utilisateurRepository) {
        this.utilisateurService = utilisateurService;
        this.photoProfilStorageService = photoProfilStorageService;
        this.activiteService = activiteService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping("/profile/edit")
    public String editProfileForm(@RequestParam(required = false) Long id, HttpSession session, Model model) {
        Long idSession = (Long) session.getAttribute(SESSION_UTILISATEUR_ID);
        if (idSession == null) {
            return REDIRECT_LOGIN;
        }
        Long idEffectif = id != null ? id : idSession;
        if (!idEffectif.equals(idSession)) {
            return REDIRECT_PROFILE_EDIT;
        }
        Utilisateur utilisateur = utilisateurService.trouverParId(idEffectif)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + idEffectif));
        utilisateur.getPrefSportives().sort(Comparator.comparing(
                pref -> pref.getNom() != null ? pref.getNom().toLowerCase() : ""));
        model.addAttribute(ATTR_UTILISATEUR, utilisateur);
        model.addAttribute("profilForm", ModifierUtilisateurRequest.fromUtilisateur(utilisateur));
        model.addAttribute("niveauxPratique", NiveauPratiqueSportive.values());
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String editProfileSubmit(@ModelAttribute("profilForm") ModifierUtilisateurRequest profilForm, HttpSession session) {
        Long idSession = (Long) session.getAttribute(SESSION_UTILISATEUR_ID);
        if (idSession == null || !profilForm.getId().equals(idSession)) {
            return REDIRECT_LOGIN;
        }
        if (profilForm.getMotdepasse() != null && profilForm.getMotdepasse().isBlank()) {
            profilForm.setMotdepasse(null);
        }
        profilForm.setPrefSportivesIds(null);
        utilisateurService.modifierUtilisateur(profilForm.getId(), profilForm);
        return REDIRECT_PROFILE_EDIT;
    }

    @PostMapping("/profile/preferences")
    public String ajouterPreferenceSportive(
            @RequestParam(required = false) Long id,
            @RequestParam String nomPreference,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long idSession = (Long) session.getAttribute(SESSION_UTILISATEUR_ID);
        if (idSession == null) {
            return REDIRECT_LOGIN;
        }
        if (id != null && !id.equals(idSession)) {
            return REDIRECT_PROFILE_EDIT;
        }

        try {
            utilisateurService.ajouterPrefSportive(idSession, nomPreference);
            redirectAttributes.addFlashAttribute("preferenceMessage", "Preference sportive ajoutee.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("preferenceErreur", exception.getMessage());
        }

        return REDIRECT_PROFILE_EDIT;
    }

    @PostMapping("/profile/preferences/{prefSportiveId}/delete")
    public String supprimerPreferenceSportive(
            @PathVariable Long prefSportiveId,
            @RequestParam(required = false) Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long idSession = (Long) session.getAttribute(SESSION_UTILISATEUR_ID);
        if (idSession == null) {
            return REDIRECT_LOGIN;
        }
        if (id != null && !id.equals(idSession)) {
            return REDIRECT_PROFILE_EDIT;
        }

        try {
            utilisateurService.supprimerPrefSportive(idSession, prefSportiveId);
            redirectAttributes.addFlashAttribute("preferenceMessage", "Preference sportive supprimee.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("preferenceErreur", exception.getMessage());
        }

        return REDIRECT_PROFILE_EDIT;
    }

    @PostMapping("/profile/photo")
    public String televerserPhotoProfil(
            @RequestParam("file") MultipartFile fichier,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long idSession = (Long) session.getAttribute(SESSION_UTILISATEUR_ID);
        if (idSession == null) {
            return REDIRECT_LOGIN;
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
        return REDIRECT_PROFILE_EDIT + "?id=" + idSession;
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        Long idSession = (Long) session.getAttribute(SESSION_UTILISATEUR_ID);
        if (idSession == null) {
            return REDIRECT_LOGIN;
        }
        Utilisateur utilisateur = utilisateurService.trouverParId(idSession)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + idSession));
        model.addAttribute(ATTR_UTILISATEUR, utilisateur);
        model.addAttribute("user", utilisateur);
        model.addAttribute(ATTR_ACTIVITES, activiteService.recupererActivitesPourProfil(utilisateur));
        model.addAttribute("amis", utilisateur.getAmis());
        model.addAttribute(ATTR_EVENEMENTS_ORGANISES, utilisateur.getEvenementsOrganises());
        model.addAttribute(ATTR_EVENEMENTS_PARTICIPE, utilisateur.getEvenementsParticipes());
        model.addAttribute("profilCompletVisible", true);
        return "profile/view";
    }

    @GetMapping("/profile/{id}")
    public String viewProfile(@PathVariable Long id, HttpSession session, Model model) {
        Long idSession = (Long) session.getAttribute(SESSION_UTILISATEUR_ID);
        if (idSession == null) {
            return REDIRECT_LOGIN;
        }
        Utilisateur utilisateur = utilisateurService.trouverParId(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + id));
        Utilisateur connecte = utilisateurService.findByIdWithAmis(idSession);

        boolean estAmi = connecte.getAmis().stream()
                .anyMatch(ami -> ami.getId().longValue() == id.longValue());

        boolean estProprietaire = idSession.equals(id);
        boolean profilCompletVisible = estProprietaire || !utilisateur.isComptePrive() || estAmi;

        model.addAttribute(ATTR_UTILISATEUR, utilisateur);
        model.addAttribute("user", connecte);
        model.addAttribute("estAmi", estAmi);
        model.addAttribute("profilCompletVisible", profilCompletVisible);

        if (profilCompletVisible) {
            model.addAttribute(ATTR_ACTIVITES, activiteService.recupererActivitesPourProfil(utilisateur));
            model.addAttribute("amis", utilisateur.getAmis());
            model.addAttribute(ATTR_EVENEMENTS_ORGANISES, utilisateur.getEvenementsOrganises());
            model.addAttribute(ATTR_EVENEMENTS_PARTICIPE, utilisateur.getEvenementsParticipes());
        } else {
            model.addAttribute(ATTR_ACTIVITES, List.of());
            model.addAttribute("amis", List.of());
            model.addAttribute(ATTR_EVENEMENTS_ORGANISES, List.of());
            model.addAttribute(ATTR_EVENEMENTS_PARTICIPE, List.of());

            Set<Long> demandesEnvoyeesIds = new LinkedHashSet<>(
                    utilisateurRepository.findDemandesAmisEnvoyeesIdsByUtilisateurId(idSession));
            Set<Long> demandesRecuesIds = utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(idSession).stream()
                    .map(Utilisateur::getId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            boolean demandeEnvoyee = demandesEnvoyeesIds.contains(id);
            boolean demandeRecue = demandesRecuesIds.contains(id);
            boolean peutEnvoyerDemandeAmiProfil = !demandeEnvoyee && !demandeRecue && !estProprietaire;
            model.addAttribute("demandeEnvoyeeProfil", demandeEnvoyee);
            model.addAttribute("demandeRecueProfil", demandeRecue);
            model.addAttribute("peutEnvoyerDemandeAmiProfil", peutEnvoyerDemandeAmiProfil);
        }
        return "profile/view";
    }
}

package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/friend/search")
public class FriendSearchController {

    private static final int NOMBRE_MAX_SUGGESTIONS = 5;
    private static final String MSG_UTILISATEUR_INTROUVABLE = "Utilisateur introuvable : ";
    private static final String FLASH_ERROR_MESSAGE = "errorMessage";
    private static final String FLASH_SUCCESS_MESSAGE = "successMessage";

    private final UtilisateurService utilisateurService;
    private final UtilisateurRepository utilisateurRepository;

    public FriendSearchController(UtilisateurService utilisateurService, UtilisateurRepository utilisateurRepository) {
        this.utilisateurService = utilisateurService;
        this.utilisateurRepository = utilisateurRepository;
    }

    @GetMapping
    public String afficherPageAmis(Authentication authentication, Model model) {
        Utilisateur utilisateurCourant = getUtilisateurCourant(authentication);
        remplirModele(model, utilisateurCourant, null);
        return "friend/search";
    }

    @GetMapping(params = "recherche")
    public String rechercherAmis(@RequestParam String recherche, Authentication authentication, Model model) {
        Utilisateur utilisateurCourant = getUtilisateurCourant(authentication);
        remplirModele(model, utilisateurCourant, recherche);
        return "friend/search";
    }

    @PostMapping("/request/{destinataireId}")
    @Transactional
    public String envoyerDemandeAmi(
            @PathVariable Long destinataireId,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) String redirectPage,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Utilisateur utilisateurCourant = getUtilisateurCourant(authentication);
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + destinataireId));

        if (utilisateurCourant.getId().equals(destinataire.getId())) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR_MESSAGE, "Vous ne pouvez pas vous ajouter vous-meme.");
            return construireRedirection(recherche, redirectPage, destinataireId);
        }

        Set<Long> amisIds = new LinkedHashSet<>(utilisateurRepository.findAmiIdsByUtilisateurId(utilisateurCourant.getId()));
        if (amisIds.contains(destinataireId)) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR_MESSAGE, "Cet utilisateur est deja votre ami.");
            return construireRedirection(recherche, redirectPage, destinataireId);
        }

        Set<Long> demandesRecuesIds = utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(utilisateurCourant.getId()).stream()
                .map(Utilisateur::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (demandesRecuesIds.contains(destinataireId)) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR_MESSAGE, "Cet utilisateur vous a deja envoye une demande.");
            return construireRedirection(recherche, redirectPage, destinataireId);
        }

        Set<Long> demandesEnvoyeesIds = new LinkedHashSet<>(
                utilisateurRepository.findDemandesAmisEnvoyeesIdsByUtilisateurId(utilisateurCourant.getId()));
        if (!demandesEnvoyeesIds.contains(destinataireId)) {
            utilisateurCourant.getDemandesAmisEnvoyees().add(destinataire);
            utilisateurRepository.save(utilisateurCourant);
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS_MESSAGE, "Demande d'ami envoyee.");
        }

        return construireRedirection(recherche, redirectPage, destinataireId);
    }

    @PostMapping("/requests/{expediteurId}/accept")
    @Transactional
    public String accepterDemandeAmi(
            @PathVariable Long expediteurId,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) String redirectPage,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Utilisateur utilisateurCourant = getUtilisateurCourant(authentication);
        Utilisateur expediteur = utilisateurRepository.findById(expediteurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + expediteurId));

        boolean demandeTrouvee = expediteur.getDemandesAmisEnvoyees()
                .removeIf(destinataire -> destinataire.getId().equals(utilisateurCourant.getId()));

        if (!demandeTrouvee) {
            redirectAttributes.addFlashAttribute(FLASH_ERROR_MESSAGE, "Cette demande d'ami est introuvable.");
            return construireRedirection(recherche, redirectPage);
        }

        if (utilisateurCourant.getAmis().stream().noneMatch(ami -> ami.getId().equals(expediteurId))) {
            utilisateurCourant.getAmis().add(expediteur);
        }
        if (expediteur.getAmis().stream().noneMatch(ami -> ami.getId().equals(utilisateurCourant.getId()))) {
            expediteur.getAmis().add(utilisateurCourant);
        }

        utilisateurRepository.save(expediteur);
        utilisateurRepository.save(utilisateurCourant);
        redirectAttributes.addFlashAttribute(FLASH_SUCCESS_MESSAGE, "Demande d'ami acceptee.");
        return construireRedirection(recherche, redirectPage);
    }

    @PostMapping("/requests/{expediteurId}/reject")
    @Transactional
    public String refuserDemandeAmi(
            @PathVariable Long expediteurId,
            @RequestParam(required = false) String recherche,
            @RequestParam(required = false) String redirectPage,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        Utilisateur utilisateurCourant = getUtilisateurCourant(authentication);
        Utilisateur expediteur = utilisateurRepository.findById(expediteurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + expediteurId));

        boolean demandeTrouvee = expediteur.getDemandesAmisEnvoyees()
                .removeIf(destinataire -> destinataire.getId().equals(utilisateurCourant.getId()));

        if (demandeTrouvee) {
            utilisateurRepository.save(expediteur);
            redirectAttributes.addFlashAttribute(FLASH_SUCCESS_MESSAGE, "Demande d'ami refusee.");
        } else {
            redirectAttributes.addFlashAttribute(FLASH_ERROR_MESSAGE, "Cette demande d'ami est introuvable.");
        }

        return construireRedirection(recherche, redirectPage);
    }

    private void remplirModele(Model model, Utilisateur utilisateurCourant, String recherche) {
        Set<Long> amisIds = new LinkedHashSet<>(utilisateurRepository.findAmiIdsByUtilisateurId(utilisateurCourant.getId()));
        Set<Long> demandesEnvoyeesIds = new LinkedHashSet<>(
                utilisateurRepository.findDemandesAmisEnvoyeesIdsByUtilisateurId(utilisateurCourant.getId()));

        List<Utilisateur> demandesRecues = utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(utilisateurCourant.getId());
        Set<Long> demandesRecuesIds = demandesRecues.stream()
                .map(Utilisateur::getId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<UtilisateurCardView> suggestions = construireSuggestions(
                utilisateurCourant, amisIds, demandesEnvoyeesIds, demandesRecuesIds);
        List<UtilisateurCardView> resultatsRecherche = construireResultatsRecherche(
                utilisateurCourant, recherche, amisIds, demandesEnvoyeesIds, demandesRecuesIds);

        model.addAttribute("utilisateurCourant", utilisateurCourant);
        model.addAttribute("recherche", recherche != null ? recherche.trim() : "");
        model.addAttribute("resultatsRecherche", resultatsRecherche);
        model.addAttribute("suggestions", suggestions);
        model.addAttribute("suggestionsVides", suggestions.isEmpty());
        model.addAttribute("demandesRecues", demandesRecues);
        model.addAttribute("demandesRecuesCount", demandesRecues.size());
    }

    private List<UtilisateurCardView> construireSuggestions(
            Utilisateur utilisateurCourant,
            Set<Long> amisIds,
            Set<Long> demandesEnvoyeesIds,
            Set<Long> demandesRecuesIds) {
        List<Utilisateur> candidats = utilisateurRepository.findAllWithPrefSportives().stream()
                .filter(utilisateur -> !utilisateur.getId().equals(utilisateurCourant.getId()))
                .filter(utilisateur -> !amisIds.contains(utilisateur.getId()))
                .filter(utilisateur -> !demandesRecuesIds.contains(utilisateur.getId()))
                .collect(Collectors.toCollection(ArrayList::new));

        Collections.shuffle(candidats);

        return candidats.stream()
                .limit(NOMBRE_MAX_SUGGESTIONS)
                .map(utilisateur -> construireCarte(utilisateur, demandesEnvoyeesIds, amisIds, demandesRecuesIds))
                .toList();
    }

    private List<UtilisateurCardView> construireResultatsRecherche(
            Utilisateur utilisateurCourant,
            String recherche,
            Set<Long> amisIds,
            Set<Long> demandesEnvoyeesIds,
            Set<Long> demandesRecuesIds) {
        if (recherche == null || recherche.isBlank()) {
            return List.of();
        }

        return utilisateurRepository.rechercherPourReseau(utilisateurCourant.getId(), recherche.trim()).stream()
                .map(utilisateur -> construireCarte(utilisateur, demandesEnvoyeesIds, amisIds, demandesRecuesIds))
                .toList();
    }

    private UtilisateurCardView construireCarte(
            Utilisateur utilisateur,
            Set<Long> demandesEnvoyeesIds,
            Set<Long> amisIds,
            Set<Long> demandesRecuesIds) {
        return new UtilisateurCardView(
                utilisateur,
                utilisateur.getPrenom() + " " + utilisateur.getNom(),
                construireSportsTexte(utilisateur),
                demandesEnvoyeesIds.contains(utilisateur.getId()),
                amisIds.contains(utilisateur.getId()),
                demandesRecuesIds.contains(utilisateur.getId()));
    }

    private String construireSportsTexte(Utilisateur utilisateur) {
        List<PrefSportive> preferences = utilisateur.getPrefSportives();
        if (preferences == null || preferences.isEmpty()) {
            return "Aucune preference sportive";
        }
        return preferences.stream()
                .map(PrefSportive::getNom)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" • "));
    }

    private Utilisateur getUtilisateurCourant(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Utilisateur non authentifie.");
        }
        return utilisateurService.trouverParEmail(authentication.getName());
    }

    private String construireRedirection(String recherche, String redirectPage) {
        return construireRedirection(recherche, redirectPage, null);
    }

    private String construireRedirection(String recherche, String redirectPage, Long idProfilCible) {
        if ("profile".equalsIgnoreCase(redirectPage) && idProfilCible != null) {
            return "redirect:/profile/" + idProfilCible;
        }
        if ("friend".equalsIgnoreCase(redirectPage)) {
            if (recherche == null || recherche.isBlank()) {
                return "redirect:/friend";
            }
            return "redirect:/friend?recherche="
                    + UriUtils.encodeQueryParam(recherche.trim(), StandardCharsets.UTF_8);
        }
        if (recherche == null || recherche.isBlank()) {
            return "redirect:/friend/search";
        }
        return "redirect:/friend/search?recherche="
                + UriUtils.encodeQueryParam(recherche.trim(), StandardCharsets.UTF_8);
    }

    public record UtilisateurCardView(
            Utilisateur utilisateur,
            String nomComplet,
            String sportsTexte,
            boolean demandeEnvoyee,
            boolean dejaAmi,
            boolean demandeRecue) {

        public boolean peutEnvoyerDemande() {
            return !demandeEnvoyee && !dejaAmi && !demandeRecue;
        }
    }

   
}

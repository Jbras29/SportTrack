package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.UtilisateurService;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/friend")
public class FriendPageController {

    private static final String REDIRECT_LOGIN = "redirect:/login";

    private final UtilisateurService utilisateurService;
    private final UtilisateurRepository utilisateurRepository;
    private final ActiviteService activiteService;

    public FriendPageController(
            UtilisateurService utilisateurService,
            UtilisateurRepository utilisateurRepository,
            ActiviteService activiteService) {
        this.utilisateurService = utilisateurService;
        this.utilisateurRepository = utilisateurRepository;
        this.activiteService = activiteService;
    }

    @GetMapping
    public String afficherPageAmis(
            @RequestParam(required = false) String recherche,
            Authentication authentication,
            Model model) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return REDIRECT_LOGIN;
        }

        Utilisateur utilisateurCourant = utilisateurService.trouverParEmailAvecAmis(authentication.getName());
        String rechercheNormalisee = recherche != null ? recherche.trim() : "";
        Map<Long, Activite> dernieresActivitesParUtilisateurId = construireDernieresActivites(utilisateurCourant);

        Comparator<Utilisateur> triAmis = Comparator
                .comparing(
                        (Utilisateur ami) -> dateDerniereActivite(dernieresActivitesParUtilisateurId.get(ami.getId())),
                        Comparator.reverseOrder())
                .thenComparing(this::construireNomComplet, String.CASE_INSENSITIVE_ORDER);

        List<FriendCardView> amis = utilisateurCourant.getAmis().stream()
                .filter(ami -> correspondRecherche(ami, rechercheNormalisee))
                .sorted(triAmis)
                .map(ami -> construireCarteAmi(ami, dernieresActivitesParUtilisateurId.get(ami.getId())))
                .toList();

        List<RequestCardView> demandesRecues = utilisateurRepository.findDemandesAmisRecuesByUtilisateurId(utilisateurCourant.getId())
                .stream()
                .sorted(Comparator.comparing(this::construireNomComplet, String.CASE_INSENSITIVE_ORDER))
                .map(this::construireCarteDemande)
                .toList();

        model.addAttribute("utilisateurCourant", utilisateurCourant);
        model.addAttribute("recherche", rechercheNormalisee);
        model.addAttribute("resumePage", construireResumePage(utilisateurCourant.getAmis().size(), amis.size(), rechercheNormalisee));
        model.addAttribute("amis", amis);
        model.addAttribute("demandesRecues", demandesRecues);
        model.addAttribute("demandesRecuesCount", demandesRecues.size());
        return "friend/index";
    }

    private Map<Long, Activite> construireDernieresActivites(Utilisateur utilisateurCourant) {
        Map<Long, Activite> dernieresActivites = new LinkedHashMap<>();
        for (Activite activite : activiteService.recupererActivitesDesAmis(utilisateurCourant)) {
            if (activite.getUtilisateur() == null || activite.getUtilisateur().getId() == null) {
                continue;
            }
            dernieresActivites.putIfAbsent(activite.getUtilisateur().getId(), activite);
        }
        return dernieresActivites;
    }

    private FriendCardView construireCarteAmi(Utilisateur ami, Activite derniereActivite) {
        boolean aDerniereActivite = derniereActivite != null;
        return new FriendCardView(
                ami.getId(),
                construireNomComplet(ami),
                ami.getEmail(),
                ami.cheminPhotoProfilAffichee(),
                "Niveau " + ami.getNiveauExperience(),
                construireStyleBanniere(ami.getId()),
                aDerniereActivite,
                estActifRecemment(derniereActivite),
                aDerniereActivite ? construireTitreActivite(derniereActivite) : "Aucune activite recente",
                aDerniereActivite
                        ? construireDetailsActivite(derniereActivite)
                        : "Cet ami n'a pas encore partage d'activite.");
    }

    private RequestCardView construireCarteDemande(Utilisateur utilisateur) {
        return new RequestCardView(
                utilisateur.getId(),
                construireNomComplet(utilisateur),
                utilisateur.cheminPhotoProfilAffichee(),
                "Niveau " + utilisateur.getNiveauExperience());
    }

    private boolean correspondRecherche(Utilisateur utilisateur, String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return true;
        }

        String terme = recherche.toLowerCase(Locale.ROOT);
        return construireNomComplet(utilisateur).toLowerCase(Locale.ROOT).contains(terme)
                || (utilisateur.getEmail() != null && utilisateur.getEmail().toLowerCase(Locale.ROOT).contains(terme));
    }

    private String construireResumePage(int nombreTotalAmis, int nombreAffiches, String recherche) {
        if (recherche == null || recherche.isBlank()) {
            return "Vous avez " + nombreTotalAmis + " ami(s) dans votre reseau Sport Track.";
        }
        if (nombreAffiches == 0) {
            return "Aucun ami ne correspond a votre recherche.";
        }
        return nombreAffiches + " ami(s) correspondent a votre recherche.";
    }

    private String construireNomComplet(Utilisateur utilisateur) {
        return (utilisateur.getPrenom() + " " + utilisateur.getNom()).trim();
    }

    private LocalDate dateDerniereActivite(Activite activite) {
        return activite != null && activite.getDate() != null ? activite.getDate() : LocalDate.MIN;
    }

    private boolean estActifRecemment(Activite activite) {
        return activite != null && activite.getDate() != null
                && !activite.getDate().isBefore(LocalDate.now().minusDays(2));
    }

    private String construireTitreActivite(Activite activite) {
        if (activite.getNom() != null && !activite.getNom().isBlank()) {
            return activite.getNom();
        }
        if (activite.getTypeSport() != null) {
            return formaterTypeSport(activite.getTypeSport());
        }
        return "Activite sportive";
    }

    private String construireDetailsActivite(Activite activite) {
        StringBuilder details = new StringBuilder();
        if (activite.getTypeSport() != null) {
            details.append(formaterTypeSport(activite.getTypeSport()));
        }

        String dateRelative = formaterDateRelative(activite.getDate());
        if (!dateRelative.isBlank()) {
            if (!details.isEmpty()) {
                details.append(" • ");
            }
            details.append(dateRelative);
        }

        String distance = formaterDistance(activite.getDistance());
        if (!distance.isBlank()) {
            if (!details.isEmpty()) {
                details.append(" • ");
            }
            details.append(distance);
        }

        return !details.isEmpty() ? details.toString() : "Activite recente";
    }

    private String formaterTypeSport(TypeSport typeSport) {
        String texte = typeSport.name().replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(texte.charAt(0)) + texte.substring(1);
    }

    private String formaterDateRelative(LocalDate date) {
        if (date == null) {
            return "";
        }
        LocalDate aujourdHui = LocalDate.now();
        long ecart = ChronoUnit.DAYS.between(date, aujourdHui);
        if (ecart <= 0) {
            return "Aujourd'hui";
        }
        if (ecart == 1) {
            return "Hier";
        }
        if (ecart < 7) {
            return "Il y a " + ecart + " jours";
        }
        return date.toString();
    }

    private String formaterDistance(Double distance) {
        if (distance == null || distance <= 0) {
            return "";
        }
        if (Math.floor(distance) == distance) {
            return ((long) Math.floor(distance)) + " km";
        }
        return String.format(Locale.FRANCE, "%.1f km", distance);
    }

    private String construireStyleBanniere(Long utilisateurId) {
        if (utilisateurId == null) {
            return "background: linear-gradient(135deg, var(--brand-blue), var(--brand-light-blue));";
        }

        return switch ((int) (utilisateurId % 4)) {
            case 0 -> "background: linear-gradient(135deg, var(--brand-blue), var(--brand-light-blue));";
            case 1 -> "background: linear-gradient(135deg, var(--brand-orange), #fcd34d);";
            case 2 -> "background: linear-gradient(135deg, var(--brand-green), #a3cc52);";
            default -> "background: linear-gradient(135deg, #4338ca, #7c3aed);";
        };
    }

    public record FriendCardView(
            Long id,
            String nomComplet,
            String email,
            String photoUrl,
            String niveauTexte,
            String styleBanniere,
            boolean aDerniereActivite,
            boolean actifRecemment,
            String derniereActiviteTitre,
            String derniereActiviteDetails) {
    }

    public record RequestCardView(
            Long id,
            String nomComplet,
            String photoUrl,
            String sousTitre) {
    }

     //On fait un endpoint pour supprimer un ami et on se redirige vers la page des amis après la suppression
    @GetMapping("/remove")
    public String supprimerAmi(@RequestParam Long id, Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            return REDIRECT_LOGIN;
        }

        Utilisateur utilisateurCourant = utilisateurService.trouverParEmail(authentication.getName());
        if (utilisateurCourant == null) {
            return REDIRECT_LOGIN;
        }

        utilisateurService.supprimerAmi(utilisateurCourant.getId(), id);
        return "redirect:/friend";
    }
}

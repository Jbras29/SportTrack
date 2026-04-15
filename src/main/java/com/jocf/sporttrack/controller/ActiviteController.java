package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerActiviteCommand;
import com.jocf.sporttrack.dto.ModifierActiviteRequest;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.enumeration.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.jocf.sporttrack.service.OpenMeteoService;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;

@Controller
@RequestMapping("/activites")
@Tag(name = "Activités", description = "Gestion des activités sportives")
public class ActiviteController {

    private final ActiviteService activiteService;
    private final UtilisateurService utilisateurService;
    private final OpenMeteoService openMeteoService;

    public ActiviteController(
            ActiviteService activiteService,
            UtilisateurService utilisateurService,
            OpenMeteoService openMeteoService) {
        this.activiteService = activiteService;
        this.utilisateurService = utilisateurService;
        this.openMeteoService = openMeteoService;
    }

    @GetMapping
    @Operation(summary = "Récupérer toutes les activités")
    public ResponseEntity<List<Activite>> getAllActivites() {
        List<Activite> activites = activiteService.recupererToutesLesActivites();
        return ResponseEntity.ok(activites);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une activité par ID (JSON)")
    @ResponseBody
    public ResponseEntity<Activite> getActiviteById(@PathVariable Long id) {
        return activiteService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Vue HTML de détail d'une activité avec météo + calories (redirigé après création) */
    @GetMapping("/{id}/detail")
    public String detailActivite(@PathVariable Long id, Model model, Authentication authentication) {
        return activiteService.trouverParId(id).map(activite -> {
            model.addAttribute("activite", activite);
            if (authentication != null && authentication.isAuthenticated()) {
                model.addAttribute("user", utilisateurService.trouverParEmail(authentication.getName()));
            }
            return "activity/detail";
        }).orElse("redirect:/profile");
    }

    @GetMapping("/utilisateur/{utilisateurId}")
    @Operation(summary = "Récupérer les activités d'un utilisateur")
    public ResponseEntity<List<Activite>> getActivitesByUtilisateur(@PathVariable Long utilisateurId) {
        try {
            List<Activite> activites = activiteService.recupererActivitesParUtilisateur(utilisateurId);
            return ResponseEntity.ok(activites);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/type/{typeSport}")
    @Operation(summary = "Récupérer les activités par type de sport")
    public ResponseEntity<List<Activite>> getActivitesByTypeSport(@PathVariable TypeSport typeSport) {
        List<Activite> activites = activiteService.recupererActivitesParTypeSport(typeSport);
        return ResponseEntity.ok(activites);
    }

    @GetMapping("/create")
    public String createActivite(Model model, Authentication authentication) {
        model.addAttribute("typesSportifs", TypeSport.values());

        if (authentication != null && authentication.isAuthenticated()) {
            Utilisateur user = utilisateurService.trouverParEmail(authentication.getName());
            model.addAttribute("user", user);
            model.addAttribute("amis", user.getAmis() != null ? user.getAmis() : Collections.emptyList());
        } else {
            model.addAttribute("amis", Collections.emptyList());
        }

        return "activity/create";
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle activité")
    public String createActiviteForm(
            @RequestParam Long utilisateurId,
            @RequestParam String nom,
            @RequestParam TypeSport typeSport,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Double distance,
            @RequestParam(required = false) Integer temps,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Integer evaluation,
            @RequestParam(required = false) List<Long> invitesIds) {
        try {
            Activite created = activiteService.creerActivite(
                    new CreerActiviteCommand(
                            utilisateurId,
                            nom,
                            typeSport,
                            date,
                            distance,
                            temps,
                            location,
                            evaluation,
                            invitesIds));
            // Redirige vers la page de confirmation qui affiche météo + calories
            return "redirect:/activites/" + created.getId() + "/detail";
        } catch (IllegalArgumentException e) {
            return "redirect:/activites/create?erreur=" + e.getMessage();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une activité")
    public ResponseEntity<Activite> updateActivite(@PathVariable Long id, @RequestBody ModifierActiviteRequest activiteDetails) {
        try {
            Activite updated = activiteService.modifierActivite(id, activiteDetails);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une activité")
    public ResponseEntity<Void> deleteActivite(@PathVariable Long id) {
        try {
            activiteService.supprimerActivite(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/kilocalories")
    @Operation(summary = "Calculer les kilocalories dépensées pour une activité")
    public ResponseEntity<Double> getKilocalories(@PathVariable Long id) {
        try {
            Double kcal = activiteService.calculerKilocalories(id);
            return ResponseEntity.ok(kcal);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/admin/recalculer-meteo-calories")
    @Operation(summary = "Recalcule les calories et météo sur toutes les activités existantes (admin)")
    public ResponseEntity<String> recalculerMeteoEtCalories() {
        int nb = activiteService.recalculerMeteoEtCaloriesPourToutesLesActivites();
        return ResponseEntity.ok("Mise à jour : " + nb + " activité(s) enrichie(s).");
    }

    /** Endpoint JSON appelé par le JS du formulaire pour prévisualiser la météo */
    @GetMapping("/api/meteo-preview")
    @ResponseBody
    @Operation(summary = "Prévisualisation météo pour le formulaire (JSON)")
    public ResponseEntity<Map<String, Object>> meteoPreview(
            @RequestParam String location,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        OpenMeteoService.WeatherInfo info = openMeteoService.getWeatherForLocationAndDate(location, date);
        Map<String, Object> result = new HashMap<>();
        if (info != null) {
            result.put("ok", true);
            result.put("condition", info.condition());
            result.put("temperature", info.temperature());
        } else {
            result.put("ok", false);
        }
        return ResponseEntity.ok(result);
    }
}
package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ActiviteService;
import com.jocf.sporttrack.service.PrefSportiveService;
import com.jocf.sporttrack.service.UtilisateurService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

import org.springframework.security.core.Authentication;


@Controller
@RequestMapping("/activites")
@Tag(name = "Activités", description = "Gestion des activités sportives")
public class ActiviteController {

    @Autowired
    private ActiviteService activiteService;

    @Autowired
    private PrefSportiveService prefSportiveService;

    @Autowired
    private UtilisateurService utilisateurService;

    @GetMapping
    @Operation(summary = "Récupérer toutes les activités")
    public ResponseEntity<List<Activite>> getAllActivites() {
        List<Activite> activites = activiteService.recupererToutesLesActivites();
        return ResponseEntity.ok(activites);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une activité par ID")
    public ResponseEntity<Activite> getActiviteById(@PathVariable Long id) {
        return activiteService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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
            @RequestParam(required = false) Integer evaluation) {
        try {
            activiteService.creerActivite(utilisateurId, nom, typeSport, date, distance, temps, location, evaluation);
            return "redirect:/profile";
        } catch (IllegalArgumentException e) {
            return "redirect:/activites/create?erreur=" + e.getMessage();
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une activité")
    public ResponseEntity<Activite> updateActivite(@PathVariable Long id, @RequestBody Activite activiteDetails) {
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
}
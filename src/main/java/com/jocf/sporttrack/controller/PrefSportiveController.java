package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.service.PrefSportiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prefsportives")
@Tag(name = "Préférences Sportives", description = "Gestion des préférences sportives")
public class PrefSportiveController {

    private final PrefSportiveService prefSportiveService;

    public PrefSportiveController(PrefSportiveService prefSportiveService) {
        this.prefSportiveService = prefSportiveService;
    }

    @GetMapping
    @Operation(summary = "Récupérer toutes les préférences sportives")
    public ResponseEntity<List<PrefSportive>> getAllPrefSportives() {
        List<PrefSportive> prefSportives = prefSportiveService.recupererToutesLesPrefSportives();
        return ResponseEntity.ok(prefSportives);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer une préférence sportive par ID")
    public ResponseEntity<PrefSportive> getPrefSportiveById(@PathVariable Long id) {
        return prefSportiveService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nom/{nom}")
    @Operation(summary = "Récupérer une préférence sportive par nom")
    public ResponseEntity<PrefSportive> getPrefSportiveByNom(@PathVariable String nom) {
        return prefSportiveService.trouverParNom(nom)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Créer une nouvelle préférence sportive")
    public ResponseEntity<PrefSportive> createPrefSportive(@RequestBody PrefSportive prefSportive) {
        PrefSportive created = prefSportiveService.creerPrefSportive(prefSportive);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une préférence sportive")
    public ResponseEntity<PrefSportive> updatePrefSportive(@PathVariable Long id, @RequestBody PrefSportive prefSportiveDetails) {
        try {
            PrefSportive updated = prefSportiveService.modifierPrefSportive(id, prefSportiveDetails);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une préférence sportive")
    public ResponseEntity<Void> deletePrefSportive(@PathVariable Long id) {
        try {
            prefSportiveService.supprimerPrefSportive(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
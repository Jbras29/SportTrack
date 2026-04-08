package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/utilisateurs")
@Tag(name = "Utilisateurs", description = "Gestion des utilisateurs")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    public UtilisateurController(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    @Operation(summary = "Récupérer tous les utilisateurs")
    public ResponseEntity<List<Utilisateur>> getAllUtilisateurs() {
        List<Utilisateur> utilisateurs = utilisateurService.recupererTousLesUtilisateurs();
        return ResponseEntity.ok(utilisateurs);
    }

    @GetMapping("/me")
    @Operation(summary = "Récupérer l'utilisateur actuellement connecté")
    public ResponseEntity<Utilisateur> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = authentication.getName();
        try {
            Utilisateur user = utilisateurService.trouverParEmail(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    @PostMapping("/auth")
    @Operation(summary = "Authentifier un utilisateur (API, sans session HTTP)")
    public ResponseEntity<String> login(@RequestParam String email, @RequestParam String motDePasse) {
        try {
            utilisateurService.connecter(email, motDePasse);
            return ResponseEntity.ok("Connexion réussie");
        } catch (Exception e) {
            return ResponseEntity.status(401).body("Email ou mot de passe incorrect");
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Récupérer un utilisateur par ID")
    public ResponseEntity<Utilisateur> getUtilisateurById(@PathVariable Long id) {
        return utilisateurService.trouverParId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity<Utilisateur> updateUtilisateur(@PathVariable Long id, @RequestBody Utilisateur utilisateurDetails) {
        try {
            Utilisateur updated = utilisateurService.modifierUtilisateur(id, utilisateurDetails);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un utilisateur")
    public ResponseEntity<Void> deleteUtilisateur(@PathVariable Long id) {
        try {
            utilisateurService.supprimerUtilisateur(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/me/amis")
    @Operation(summary = "Récupérer la liste des amis de l'utilisateur connecté")
    public ResponseEntity<List<Map<String, Object>>> getMesAmis() {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        try {
            // 1. Essayer de trouver l'utilisateur.
            // S'il n'existe pas, cela va déclencher UsernameNotFoundException
            Utilisateur u = utilisateurService.trouverParEmail(email);

            // 2. Si on arrive ici, c'est que l'utilisateur a été trouvé !
            List<Map<String, Object>> result = new ArrayList<>();
            for (Utilisateur ami : u.getAmis()) {
                Map<String, Object> map = new HashMap<>();
                map.put("id", ami.getId());
                map.put("nom", ami.getNom());
                result.add(map);
            }
            return ResponseEntity.ok(result);

        } catch (UsernameNotFoundException e) {
            // 3. Si l'exception est levée, on l'attrape ici et on renvoie une erreur 401
            return ResponseEntity.status(401).build();
        }
    }
}
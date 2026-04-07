package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ActiviteService {

    private final ActiviteRepository activiteRepository;
    private final UtilisateurRepository utilisateurRepository;

    public ActiviteService(ActiviteRepository activiteRepository, UtilisateurRepository utilisateurRepository) {
        this.activiteRepository = activiteRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<Activite> recupererToutesLesActivites() {
        return activiteRepository.findAll();
    }

    public Optional<Activite> trouverParId(Long id) {
        return activiteRepository.findById(id);
    }

    public List<Activite> recupererActivitesParUtilisateur(Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));
        return activiteRepository.findByUtilisateur(utilisateur);
    }

    public List<Activite> recupererActivitesParTypeSport(TypeSport typeSport) {
        return activiteRepository.findByTypeSport(typeSport);
    }

    public Activite creerActivite(Long utilisateurId, String nom, TypeSport typeSport, LocalDate date) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

        Activite activite = Activite.builder()
                .nom(nom)
                .typeSport(typeSport)
                .date(date)
                .utilisateur(utilisateur)
                .build();

        return activiteRepository.save(activite);
    }

    public Activite modifierActivite(Long id, Activite activiteDetails) {
        Activite activite = activiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable : " + id));

        activite.setNom(activiteDetails.getNom());
        activite.setTypeSport(activiteDetails.getTypeSport());
        activite.setDistance(activiteDetails.getDistance());
        activite.setTemps(activiteDetails.getTemps());
        activite.setDate(activiteDetails.getDate());
        activite.setLocation(activiteDetails.getLocation());
        activite.setEvaluation(activiteDetails.getEvaluation());

        return activiteRepository.save(activite);
    }

    public void supprimerActivite(Long id) {
        if (!activiteRepository.existsById(id)) {
            throw new IllegalArgumentException("Activite introuvable : " + id);
        }
        activiteRepository.deleteById(id);
    }
}
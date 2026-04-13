package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.EvenementRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AnnonceService {

    private final AnnonceRepository annonceRepository;
    private final EvenementRepository evenementRepository;
    private final UtilisateurRepository utilisateurRepository;

    public AnnonceService(AnnonceRepository annonceRepository, EvenementRepository evenementRepository, UtilisateurRepository utilisateurRepository) {
        this.annonceRepository = annonceRepository;
        this.evenementRepository = evenementRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    public List<Annonce> recupererToutesLesAnnonces() {
        return annonceRepository.findAll();
    }

    public Optional<Annonce> trouverParId(Long id) {
        return annonceRepository.findById(id);
    }

    public List<Annonce> recupererAnnoncesParEvenement(Long evenementId) {
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new IllegalArgumentException("Evenement introuvable : " + evenementId));
        return annonceRepository.findByEvenement(evenement);
    }

    public List<Annonce> recupererAnnoncesParParticipant(Long utilisateurId) {
    Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
        .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

    return utilisateur.getEvenementsParticipes().stream()
        .flatMap(evenement -> annonceRepository.findByEvenement(evenement).stream())
        .toList();
}

    public Annonce creerAnnonce(Long evenementId, Long organisateurId, String message) {
        Evenement evenement = evenementRepository.findById(evenementId)
            .orElseThrow(() -> new IllegalArgumentException("Evenement introuvable : " + evenementId));
    
        if (!evenement.getOrganisateur().getId().equals(organisateurId)) {
            throw new IllegalArgumentException("Seul l'organisateur peut publier une annonce.");
        }
    
        Annonce annonce = Annonce.builder()
            .message(message)
            .date(LocalDateTime.now())
            .evenement(evenement)
            .build();
    
        return annonceRepository.save(annonce);
    }

    public Annonce modifierAnnonce(Long id, String nouveauMessage) {
        Annonce annonce = annonceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annonce introuvable : " + id));

        annonce.setMessage(nouveauMessage);
        return annonceRepository.save(annonce);
    }

    public void supprimerAnnonce(Long id) {
        if (!annonceRepository.existsById(id)) {
            throw new IllegalArgumentException("Annonce introuvable : " + id);
        }
        annonceRepository.deleteById(id);
    }
}
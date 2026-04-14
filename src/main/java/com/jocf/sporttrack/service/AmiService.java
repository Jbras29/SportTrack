package com.jocf.sporttrack.service;

import org.springframework.stereotype.Service;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;

@Service
public class AmiService {

    private final UtilisateurRepository utilisateurRepository;

    public AmiService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public void envoyerDemandeAmi(Long expediteurId, Long destinataireId) {
        Utilisateur expediteur = utilisateurRepository.findById(expediteurId).orElseThrow();
        Utilisateur destinataire = utilisateurRepository.findById(destinataireId).orElseThrow();
        expediteur.getDemandesAmisEnvoyees().add(destinataire);
        utilisateurRepository.save(expediteur);
    }
}

package com.jocf.sporttrack.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.springframework.boot.test.context.SpringBootTest;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;

import jakarta.transaction.Transactional;

@SpringBootTest
@Transactional
class FriendRequestTest {

    @Autowired UtilisateurRepository utilisateurRepository;
    @Autowired UtilisateurService utilisateurService;
    @Autowired AmiService friendService;

    @Test
    void recherche_utilisateur_existant_et_envoi_demande_ami() {
        Utilisateur alice = utilisateurRepository.save(
            Utilisateur.builder().nom("Dupont").prenom("Alice").email("alice@mail.com").motdepasse("1234").build()
        );
        Utilisateur bob = utilisateurRepository.save(
            Utilisateur.builder().nom("Martin").prenom("Bob").email("bob@mail.com").motdepasse("1234").build()
        );

        Utilisateur results = utilisateurService.trouverParEmail("bob@mail.com");
        friendService.envoyerDemandeAmi(alice.getId(), bob.getId());

        assertNotNull(results);
        assertEquals("Bob", results.getPrenom());

        Utilisateur aliceMaj = utilisateurRepository.findById(alice.getId()).orElseThrow();
        assertTrue(aliceMaj.getDemandesAmisEnvoyees().stream()
            .anyMatch(u -> u.getId().equals(bob.getId())));
    }

    @Test
void recherche_utilisateur_inexistant() {
    // aucun utilisateur "Inconnu" en base

    
    List<Utilisateur> results = utilisateurService.rechercherParNom("Inconnu");

    // Then
    assertTrue(results.isEmpty());
}
}
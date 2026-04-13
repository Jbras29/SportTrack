package com.jocf.sporttrack.model;

import com.jocf.sporttrack.dto.CreerEvenementRequest;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.EvenementRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.EvenementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import static org.junit.jupiter.api.Assertions.*;

class EvenementTest {

    /**
     * Teste le bon fonctionnement du Builder et des valeurs par défaut.
     */
    @Test
    void testEvenementBuilderEtDefaults() {
        // GIVEN: Création d'un utilisateur pour l'organisateur
        Utilisateur organisateur = Utilisateur.builder().id(1L).nom("Alice").build();
        LocalDateTime dateEv = LocalDateTime.now().plusDays(2);

        // WHEN: Utilisation du builder pour créer l'événement
        Evenement evenement = Evenement.builder()
                .id(10L)
                .nom("Tournoi de Basket")
                .description("Un tournoi amical")
                .date(dateEv)
                .organisateur(organisateur)
                .build();

        // THEN: Vérification des champs simples
        assertEquals(10L, evenement.getId());
        assertEquals("Tournoi de Basket", evenement.getNom());
        assertEquals(organisateur, evenement.getOrganisateur());

        // THEN: Vérification du comportement de @Builder.Default
        assertNotNull(evenement.getParticipants(), "La liste des participants ne doit pas être null");
        assertNotNull(evenement.getAnnonces(), "La liste des annonces ne doit pas être null");
        assertTrue(evenement.getParticipants().isEmpty());
    }

    /**
     * Teste la gestion des relations (Ajout de participants et annonces).
     */
    @Test
    void testEvenementRelations() {
        // GIVEN
        Evenement evenement = new Evenement();
        evenement.setParticipants(new ArrayList<>());
        evenement.setAnnonces(new ArrayList<>());

        Utilisateur u1 = Utilisateur.builder().id(1L).build();
        Annonce a1 = Annonce.builder().id(1L).message("Bienvenue").build();

        // WHEN: Ajout dans les listes
        evenement.getParticipants().add(u1);
        evenement.getAnnonces().add(a1);

        // THEN
        assertEquals(1, evenement.getParticipants().size());
        assertEquals(1, evenement.getAnnonces().size());
        assertEquals(u1, evenement.getParticipants().get(0));
    }

    /**
     * Teste l'égalité et le hashCode (vérification des exclusions).
     */
    @Test
    void testEqualsAndHashCode() {
        LocalDateTime date = LocalDateTime.now();
        Utilisateur organisateur = Utilisateur.builder().id(1L).build();

        Evenement e1 = Evenement.builder().id(1L).nom("Ev1").date(date).organisateur(organisateur).build();
        Evenement e2 = Evenement.builder().id(1L).nom("Ev1").date(date).organisateur(organisateur).build();

        // Même si les listes de participants sont différentes, ils doivent être égaux grâce à l'ID
        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
    }
}
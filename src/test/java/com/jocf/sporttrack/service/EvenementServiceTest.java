package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.CreerEvenementRequest;
import com.jocf.sporttrack.dto.ModifierEvenementRequest;
import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
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

@ExtendWith(MockitoExtension.class)
public class EvenementServiceTest {
    @Mock
    private EvenementRepository evenementRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private AnnonceRepository annonceRepository;

    @InjectMocks
    private EvenementService evenementService;


    @Test
    void creerEvenement_Succes() {
        // GIVEN
        Long userId = 1L;
        Utilisateur organisateur = Utilisateur.builder().id(userId).nom("Boss").build();

        CreerEvenementRequest req = new CreerEvenementRequest("Randonnée", "Desc", LocalDateTime.now(), new ArrayList<>());

        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(organisateur));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        Evenement result = evenementService.creerEvenement(userId, req);

        // THEN
        assertNotNull(result);
        assertEquals("Randonnée", result.getNom());
        assertEquals(organisateur, result.getOrganisateur());
        assertTrue(result.getParticipants().contains(organisateur), "L'organisateur doit être participant");
        verify(evenementRepository).save(any(Evenement.class));
    }

    @Test
    void creerEvenement_AvecParticipants_CouvertureBoucle() {
        // GIVEN
        Long organisateurId = 1L;
        Long participantId = 2L;

        Utilisateur organisateur = Utilisateur.builder().id(organisateurId).build();
        Utilisateur participant = Utilisateur.builder().id(participantId).build();

        // Créer un participant factice pour la liste
        CreerEvenementRequest.ParticipantIdRef pRef = new CreerEvenementRequest.ParticipantIdRef(participantId);
        List<CreerEvenementRequest.ParticipantIdRef> participantsReq = List.of(pRef);

        CreerEvenementRequest req = new CreerEvenementRequest(
                "Match de Foot", "Description", LocalDateTime.now().plusDays(1), participantsReq
        );

        when(utilisateurRepository.findById(organisateurId)).thenReturn(Optional.of(organisateur));
        when(utilisateurRepository.findById(participantId)).thenReturn(Optional.of(participant));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        Evenement result = evenementService.creerEvenement(organisateurId, req);

        // THEN
        // Vérifier que la liste contient l'organisateur ET le participant
        assertEquals(2, result.getParticipants().size());
        assertTrue(result.getParticipants().contains(participant));
        verify(utilisateurRepository).findById(participantId);
    }


    @Test
    void creerEvenement_AvecParticipantIdNull_CouvertureBranche() {
        // GIVEN : Un organisateur et une requête contenant un participant avec ID null
        Long organisateurId = 1L;
        Utilisateur organisateur = Utilisateur.builder().id(organisateurId).build();

        // On crée un participant dont l'ID est explicitement null
        CreerEvenementRequest.ParticipantIdRef participantNull = new CreerEvenementRequest.ParticipantIdRef(null);
        List<CreerEvenementRequest.ParticipantIdRef> participantsReq = List.of(participantNull);

        CreerEvenementRequest req = new CreerEvenementRequest(
                "Événement Test",
                "Description",
                LocalDateTime.now().plusDays(1),
                participantsReq
        );

        // Mock : Seul l'organisateur est trouvé
        when(utilisateurRepository.findById(organisateurId)).thenReturn(Optional.of(organisateur));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN : On crée l'événement
        Evenement result = evenementService.creerEvenement(organisateurId, req);

        // THEN : La liste ne doit contenir que l'organisateur, l'ID null doit être ignoré
        assertEquals(1, result.getParticipants().size());
        assertTrue(result.getParticipants().contains(organisateur));

        // Vérifier que findById n'a jamais été appelé pour un ID null
        verify(utilisateurRepository, never()).findById(null);
    }

    @Test
    void creerEvenement_DescriptionNull_DevraitEtreVide() {
        // GIVEN
        Long id = 1L;
        Utilisateur user = Utilisateur.builder().id(id).build();
        // Description est explicitement null
        CreerEvenementRequest req = new CreerEvenementRequest("Titre", null, LocalDateTime.now().plusDays(1), new ArrayList<>());

        when(utilisateurRepository.findById(id)).thenReturn(Optional.of(user));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        Evenement result = evenementService.creerEvenement(id, req);

        // THEN
        assertEquals("", result.getDescription()); // Doit retourner une chaîne vide (应返回空字符串)
    }

    @Test
    void creerEvenement_DescriptionVide_DevraitEtreVide() {
        // GIVEN
        Long id = 1L;
        Utilisateur user = Utilisateur.builder().id(id).build();
        // Description avec des espaces
        CreerEvenementRequest req = new CreerEvenementRequest("Titre", "   ", LocalDateTime.now().plusDays(1), new ArrayList<>());

        when(utilisateurRepository.findById(id)).thenReturn(Optional.of(user));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        Evenement result = evenementService.creerEvenement(id, req);

        // THEN
        assertEquals("", result.getDescription());
    }


    @Test
    void rejoindreEvenement_Succes() {
        // GIVEN
        Utilisateur user = Utilisateur.builder().id(2L).build();
        Evenement evenement = Evenement.builder().id(10L).participants(new ArrayList<>()).build();

        when(evenementRepository.findById(10L)).thenReturn(Optional.of(evenement));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        Evenement result = evenementService.rejoindreEvenement(10L, user);

        // THEN
        assertTrue(result.getParticipants().contains(user));
        verify(evenementRepository).save(evenement);
    }

    @Test
    void rejoindreEvenement_Erreur_DejaParticipant() {
        // GIVEN
        Utilisateur user = Utilisateur.builder().id(2L).build();
        List<Utilisateur> participants = new ArrayList<>();
        participants.add(user);
        Evenement evenement = Evenement.builder().id(10L).participants(participants).build();

        when(evenementRepository.findById(10L)).thenReturn(Optional.of(evenement));

        // WHEN & THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            evenementService.rejoindreEvenement(10L, user);
        });
        assertEquals("Vous participez déjà à cet événement", ex.getMessage());
    }

    /**
     * Test de publication d'une annonce.
     */
    @Test
    void ajouterAnnonce_Succes() {
        // GIVEN
        Evenement ev = Evenement.builder().id(100L).build();
        when(evenementRepository.findById(100L)).thenReturn(Optional.of(ev));
        when(annonceRepository.save(any(Annonce.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        Annonce result = evenementService.ajouterAnnonce(100L, "Rendez-vous à 10h");

        // THEN
        assertNotNull(result);
        assertEquals("Rendez-vous à 10h", result.getMessage());
        assertEquals(ev, result.getEvenement());
        verify(annonceRepository).save(any(Annonce.class));
    }

    /**
     * Test de retrait d'un participant.
     */
    @Test
    void retirerParticipant_Succes() {
        // GIVEN
        Utilisateur p1 = Utilisateur.builder().id(1L).build();
        Utilisateur p2 = Utilisateur.builder().id(2L).build();
        List<Utilisateur> list = new ArrayList<>();
        list.add(p1); list.add(p2);

        Evenement ev = Evenement.builder().id(50L).participants(list).build();
        when(evenementRepository.findById(50L)).thenReturn(Optional.of(ev));

        // WHEN
        evenementService.retirerParticipant(50L, 1L);

        // THEN
        assertEquals(1, ev.getParticipants().size());
        assertEquals(2L, ev.getParticipants().get(0).getId());
        verify(evenementRepository).save(ev);
    }



    /**
     * Test de récupération des événements par organisateur.
     */
    @Test
    void obtenirEvenementsParOrganisateur_Succes() {
        // GIVEN
        Long userId = 1L;
        Utilisateur organisateur = Utilisateur.builder().id(userId).build();
        List<Evenement> evenements = List.of(new Evenement(), new Evenement());

        when(utilisateurRepository.findById(userId)).thenReturn(Optional.of(organisateur));
        when(evenementRepository.findByOrganisateur(organisateur)).thenReturn(evenements);

        // WHEN
        List<Evenement> result = evenementService.obtenirEvenementsParOrganisateur(userId);

        // THEN
        assertEquals(2, result.size());
        verify(evenementRepository).findByOrganisateur(organisateur);
    }

    /**
     * Test de récupération de tous les événements.
     */
    @Test
    void obtenirTousLesEvenements_Succes() {
        // GIVEN
        when(evenementRepository.findAll()).thenReturn(List.of(new Evenement()));

        // WHEN
        List<Evenement> result = evenementService.obtenirTousLesEvenements();

        // THEN
        assertFalse(result.isEmpty());
        verify(evenementRepository).findAll();
    }

    /**
     * Test de suppression d'une annonce (Succès et Erreur).
     */
    @Test
    void supprimerAnnonce_Succes() {
        // GIVEN
        Long id = 100L;
        when(annonceRepository.existsById(id)).thenReturn(true);

        // WHEN
        evenementService.supprimerAnnonce(id);

        // THEN
        verify(annonceRepository).deleteById(id);
    }

    @Test
    void supprimerAnnonce_Echec_Introuvable() {
        // GIVEN
        Long id = 999L;
        when(annonceRepository.existsById(id)).thenReturn(false);

        // WHEN & THEN
        assertThrows(RuntimeException.class, () -> evenementService.supprimerAnnonce(id));
        verify(annonceRepository, never()).deleteById(id);
    }

    /**
     * Test de modification d'un événement.
     */
    @Test
    void modifierEvenement_Succes() {
        // GIVEN
        Long id = 5L;
        Evenement existant = Evenement.builder().id(id).nom("Vieux Nom").build();
        ModifierEvenementRequest req = new ModifierEvenementRequest("Nouveau Nom","Nouveau description",LocalDateTime.now().plusDays(1));

        when(evenementRepository.findById(id)).thenReturn(Optional.of(existant));
        when(evenementRepository.save(any(Evenement.class))).thenAnswer(i -> i.getArgument(0));

        // WHEN
        Evenement result = evenementService.modifierEvenement(id, req);

        // THEN
        assertEquals("Nouveau Nom", result.getNom());
        assertEquals("Nouveau description", result.getDescription());
        verify(evenementRepository).save(existant);
    }

    /**
     * Test de suppression d'un événement.
     */
    @Test
    void supprimer_Succes() {
        // GIVEN
        Long id = 1L;

        // WHEN
        evenementService.supprimer(id);

        // THEN
        verify(evenementRepository).deleteById(id);
    }

    /**
     * Test pour quitter un événement.
     */
    @Test
    void quitterEvenement_Succes() {
        // GIVEN : Un événement avec deux participants

        Long idEv = 10L;
        Long idUserQuiQuitte = 1L;

        Utilisateur user1 = Utilisateur.builder().id(idUserQuiQuitte).build();
        Utilisateur user2 = Utilisateur.builder().id(2L).build();


        List<Utilisateur> participants = new ArrayList<>();
        participants.add(user1);
        participants.add(user2);

        Evenement evenement = Evenement.builder()
                .id(idEv)
                .participants(participants)
                .build();

        // Mock : Quand on cherche l'événement, on le trouve
        when(evenementRepository.findById(idEv)).thenReturn(Optional.of(evenement));

        // WHEN : L'utilisateur 1 quitte l'événement

        evenementService.quitterEvenement(idEv, idUserQuiQuitte);

        // THEN : Il ne doit rester que l'utilisateur 2

        assertEquals(1, evenement.getParticipants().size());
        assertFalse(evenement.getParticipants().contains(user1));
        assertTrue(evenement.getParticipants().contains(user2));

        // Vérifier que la sauvegarde a été appelée
        verify(evenementRepository).save(evenement);
    }
}

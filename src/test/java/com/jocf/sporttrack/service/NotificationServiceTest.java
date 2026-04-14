package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.NotificationItem;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.NotificationType;
import com.jocf.sporttrack.model.TypeCommentaire;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.repository.ChallengeSaisieQuotidienneRepository;
import com.jocf.sporttrack.repository.CommentaireRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class NotificationServiceTest {

    @Test
    void listerPourUtilisateur_agregeLesSources() {
        CommentaireRepository commentaireRepository = Mockito.mock(CommentaireRepository.class);
        AnnonceRepository annonceRepository = Mockito.mock(AnnonceRepository.class);
        ActiviteRepository activiteRepository = Mockito.mock(ActiviteRepository.class);
        ChallengeRepository challengeRepository = Mockito.mock(ChallengeRepository.class);
        ChallengeSaisieQuotidienneRepository saisieRepository = Mockito.mock(ChallengeSaisieQuotidienneRepository.class);
        UtilisateurRepository utilisateurRepository = Mockito.mock(UtilisateurRepository.class);
        NotificationService self = Mockito.mock(NotificationService.class);
        NotificationService service = new NotificationService(
                self, commentaireRepository, annonceRepository, activiteRepository, challengeRepository, saisieRepository, utilisateurRepository);
        ReflectionTestUtils.setField(service, "joursSansActiviteRappel", 7);

        Utilisateur owner = Utilisateur.builder()
                .id(1L).nom("Owner").prenom("One").email("owner@test.com").motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .derniereConsultationNotifications(LocalDateTime.now().minusDays(2))
                .build();
        Utilisateur auteur = Utilisateur.builder()
                .id(2L).nom("Doe").prenom("Jane").email("jane@test.com").motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        Activite activite = Activite.builder().id(4L).nom("Run").date(LocalDate.now().minusDays(10)).utilisateur(owner).build();
        Commentaire reaction = Commentaire.builder()
                .id(5L).type(TypeCommentaire.REACTION).message("👍").auteur(auteur).activite(activite).dateCreation(LocalDateTime.now().minusHours(3)).build();
        Commentaire reponse = Commentaire.builder()
                .id(6L).type(TypeCommentaire.MESSAGE).message("Bravo").auteur(auteur).activite(activite).dateCreation(LocalDateTime.now().minusHours(2)).build();
        Evenement evenement = Evenement.builder().id(7L).nom("Trail").participants(List.of(owner)).build();
        Annonce annonce = Annonce.builder().id(8L).message("Annonce").date(LocalDateTime.now().minusHours(1)).evenement(evenement).build();
        Challenge challenge = Challenge.builder()
                .id(9L)
                .nom("Défi")
                .dateDebut(Date.valueOf(LocalDate.now().minusDays(1)))
                .dateFin(Date.valueOf(LocalDate.now().plusDays(1)))
                .build();

        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(commentaireRepository.findPourProprietaireActiviteEtType(1L, TypeCommentaire.REACTION)).thenReturn(List.of(reaction));
        when(commentaireRepository.findPourProprietaireActiviteEtType(1L, TypeCommentaire.MESSAGE)).thenReturn(List.of(reponse));
        when(annonceRepository.findAnnoncesPourEvenementsOuUtilisateurParticipe(1L)).thenReturn(List.of(annonce));
        when(activiteRepository.findTopByUtilisateurOrderByDateDesc(owner)).thenReturn(Optional.of(activite));
        when(challengeRepository.findByParticipants_IdOrderByDateFinAsc(1L)).thenReturn(List.of(challenge));
        when(saisieRepository.existsByChallenge_IdAndUtilisateur_IdAndJour(9L, 1L, LocalDate.now())).thenReturn(false);

        List<NotificationItem> items = service.listerPourUtilisateur(1L, owner.getDerniereConsultationNotifications());

        assertThat(items).extracting(NotificationItem::type)
                .contains(NotificationType.REACTION, NotificationType.REPONSE_ACTIVITE,
                        NotificationType.ANNONCE_EVENEMENT, NotificationType.RAPPEL_ACTIVITE,
                        NotificationType.RAPPEL_CHALLENGE_QUOTIDIEN);
    }

    @Test
    void compterNotificationsNonLues_utiliseLeSelfProxy() {
        CommentaireRepository commentaireRepository = Mockito.mock(CommentaireRepository.class);
        AnnonceRepository annonceRepository = Mockito.mock(AnnonceRepository.class);
        ActiviteRepository activiteRepository = Mockito.mock(ActiviteRepository.class);
        ChallengeRepository challengeRepository = Mockito.mock(ChallengeRepository.class);
        ChallengeSaisieQuotidienneRepository saisieRepository = Mockito.mock(ChallengeSaisieQuotidienneRepository.class);
        UtilisateurRepository utilisateurRepository = Mockito.mock(UtilisateurRepository.class);
        NotificationService self = Mockito.mock(NotificationService.class);
        NotificationService service = new NotificationService(
                self, commentaireRepository, annonceRepository, activiteRepository, challengeRepository, saisieRepository, utilisateurRepository);
        Utilisateur owner = Utilisateur.builder()
                .id(1L).nom("Owner").prenom("One").email("owner@test.com").motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .derniereConsultationNotifications(LocalDateTime.now().minusHours(1))
                .build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(self.listerPourUtilisateur(1L, owner.getDerniereConsultationNotifications())).thenReturn(List.of(
                new NotificationItem(NotificationType.REACTION, "t", "d", LocalDateTime.now(), "/x", 1L, null, true),
                new NotificationItem(NotificationType.REACTION, "t2", "d2", LocalDateTime.now().minusDays(2), "/x", 2L, null, false)
        ));

        long count = service.compterNotificationsNonLues(1L);

        assertThat(count).isEqualTo(1L);
    }
}

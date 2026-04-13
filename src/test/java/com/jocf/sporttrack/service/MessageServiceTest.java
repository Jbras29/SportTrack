package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Message;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final Long USER_A_ID = 1L;
    private static final Long USER_B_ID = 2L;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private MessageService messageService;

    @BeforeEach
    void lenientSave() {
        lenient().when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(messageRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Utilisateur user(Long id) {
        return Utilisateur.builder()
                .id(id)
                .nom("N" + id)
                .prenom("P" + id)
                .email("u" + id + "@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
    }

    @Nested
    class EnvoyerMessage {

        @Test
        void expediteurNull_leveIllegalArgumentException() {
            Utilisateur destinataire = user(USER_B_ID);
            Executable appel = () -> messageService.envoyerMessage(null, destinataire, "hi");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("null");
        }

        @Test
        void destinataireNull_leveIllegalArgumentException() {
            Utilisateur expediteur = user(USER_A_ID);
            Executable appel = () -> messageService.envoyerMessage(expediteur, null, "hi");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("null");
        }

        @Test
        void contenuNull_leveIllegalArgumentException() {
            Utilisateur expediteur = user(USER_A_ID);
            Utilisateur destinataire = user(USER_B_ID);
            Executable appel = () -> messageService.envoyerMessage(expediteur, destinataire, null);
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("vide");
        }

        @Test
        void contenuVide_leveIllegalArgumentException() {
            Utilisateur expediteur = user(USER_A_ID);
            Utilisateur destinataire = user(USER_B_ID);
            Executable appel = () -> messageService.envoyerMessage(expediteur, destinataire, "   ");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("vide");
        }

        @Test
        void ok_trimContenu_persiste() {
            Utilisateur a = user(USER_A_ID);
            Utilisateur b = user(USER_B_ID);

            messageService.envoyerMessage(a, b, "  salut  ");

            ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
            verify(messageRepository).save(captor.capture());
            Message saved = captor.getValue();
            assertThat(saved.getExpediteur()).isEqualTo(a);
            assertThat(saved.getDestinataire()).isEqualTo(b);
            assertThat(saved.getContenu()).isEqualTo("salut");
            assertThat(saved.isLu()).isFalse();
            assertThat(saved.getDateEnvoi()).isNotNull();
        }
    }

    @Nested
    class GetConversation {

        @Test
        void userNull_retourneListeVide() {
            assertThat(messageService.getConversation(null, user(USER_B_ID))).isEmpty();
            verify(messageRepository, never()).findConversationBetweenUsers(any(), any());
        }

        @Test
        void delegueAuRepository() {
            Utilisateur u1 = user(USER_A_ID);
            Utilisateur u2 = user(USER_B_ID);
            Message m = Message.builder().id(1L).build();
            when(messageRepository.findConversationBetweenUsers(u1, u2)).thenReturn(List.of(m));

            assertThat(messageService.getConversation(u1, u2)).containsExactly(m);
        }
    }

    @Nested
    class GetMessagesRecusOuEnvoyes {

        @Test
        void getMessagesRecus_destinataireNull_retourneVide() {
            assertThat(messageService.getMessagesRecus(null)).isEmpty();
            verify(messageRepository, never()).findByDestinataireOrderByDateEnvoiDesc(any());
        }

        @Test
        void getMessagesEnvoyes_expediteurNull_retourneVide() {
            assertThat(messageService.getMessagesEnvoyes(null)).isEmpty();
            verify(messageRepository, never()).findByExpediteurOrderByDateEnvoiDesc(any());
        }
    }

    @Nested
    class MarquerCommeLu {

        @Test
        void messageNull_nePersistePas() {
            messageService.marquerCommeLu(null);
            verify(messageRepository, never()).save(any());
        }

        @Test
        void dejaLu_nePersistePas() {
            Message m = Message.builder().id(5L).lu(true).build();
            messageService.marquerCommeLu(m);
            verify(messageRepository, never()).save(any());
        }

        @Test
        void nonLu_persiste() {
            Message m = Message.builder()
                    .id(5L)
                    .expediteur(user(USER_A_ID))
                    .destinataire(user(USER_B_ID))
                    .contenu("x")
                    .dateEnvoi(LocalDateTime.now())
                    .lu(false)
                    .build();
            messageService.marquerCommeLu(m);
            assertThat(m.isLu()).isTrue();
            verify(messageRepository).save(m);
        }
    }

    @Nested
    class GetConversationEtMarquerRecusCommeLus {

        @Test
        void utilisateurNull_retourneVide() {
            assertThat(messageService.getConversationEtMarquerRecusCommeLus(null, user(USER_B_ID))).isEmpty();
            verify(messageRepository, never()).findConversationBetweenUsers(any(), any());
        }

        @Test
        void marqueSeulementLesNonLusRecusParConnecte_etSaveAll() {
            Utilisateur connecte = user(USER_A_ID);
            Utilisateur autre = user(USER_B_ID);

            Message recuNonLu = Message.builder()
                    .id(1L)
                    .expediteur(autre)
                    .destinataire(connecte)
                    .contenu("a")
                    .dateEnvoi(LocalDateTime.now())
                    .lu(false)
                    .build();
            Message envoyeNonLu = Message.builder()
                    .id(2L)
                    .expediteur(connecte)
                    .destinataire(autre)
                    .contenu("b")
                    .dateEnvoi(LocalDateTime.now())
                    .lu(false)
                    .build();

            when(messageRepository.findConversationBetweenUsers(connecte, autre))
                    .thenReturn(List.of(recuNonLu, envoyeNonLu));

            List<Message> result = messageService.getConversationEtMarquerRecusCommeLus(connecte, autre);

            assertThat(result).containsExactly(recuNonLu, envoyeNonLu);
            assertThat(recuNonLu.isLu()).isTrue();
            assertThat(recuNonLu.getDateLu()).isNotNull();
            assertThat(envoyeNonLu.isLu()).isFalse();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            verify(messageRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).containsExactly(recuNonLu);
        }

        @Test
        void rienAPersister_neAppellePasSaveAll() {
            Utilisateur connecte = user(USER_A_ID);
            Utilisateur autre = user(USER_B_ID);
            when(messageRepository.findConversationBetweenUsers(connecte, autre)).thenReturn(List.of());

            messageService.getConversationEtMarquerRecusCommeLus(connecte, autre);

            verify(messageRepository, never()).saveAll(any());
        }
    }

    @Nested
    class MarquerTousCommeLus {

        @Test
        void destinataireNull_neFaitRien() {
            messageService.marquerTousCommeLus(null);
            verify(messageRepository, never()).findByDestinataireAndLuFalse(any());
        }

        @Test
        void listeVide_neAppellePasSaveAll() {
            Utilisateur d = user(USER_B_ID);
            when(messageRepository.findByDestinataireAndLuFalse(d)).thenReturn(List.of());

            messageService.marquerTousCommeLus(d);

            verify(messageRepository, never()).saveAll(any());
        }

        @Test
        void marqueTousEtSaveAll() {
            Utilisateur d = user(USER_B_ID);
            Message m1 = Message.builder().id(1L).lu(false).destinataire(d).expediteur(user(USER_A_ID))
                    .contenu("x").dateEnvoi(LocalDateTime.now()).build();
            Message m2 = Message.builder().id(2L).lu(false).destinataire(d).expediteur(user(USER_A_ID))
                    .contenu("y").dateEnvoi(LocalDateTime.now()).build();
            when(messageRepository.findByDestinataireAndLuFalse(d)).thenReturn(List.of(m1, m2));

            messageService.marquerTousCommeLus(d);

            assertThat(m1.isLu()).isTrue();
            assertThat(m2.isLu()).isTrue();
            verify(messageRepository).saveAll(List.of(m1, m2));
        }
    }

    @Nested
    class CompterMessagesNonLus {

        @Test
        void destinataireNull_retourneZero() {
            assertThat(messageService.compterMessagesNonLus(null)).isZero();
        }

        @Test
        void delegueAuRepository() {
            Utilisateur d = user(USER_B_ID);
            when(messageRepository.countByDestinataireAndLuFalse(d)).thenReturn(3L);

            assertThat(messageService.compterMessagesNonLus(d)).isEqualTo(3L);
        }
    }

    @Nested
    class GetDerniersMessagesAvecChaqueUtilisateur {

        @Test
        void userNull_retourneVide() {
            assertThat(messageService.getDerniersMessagesAvecChaqueUtilisateur(null)).isEmpty();
        }

        @Test
        void fusionneDestinatairesEtExpediteurs_deduplique_trieParDateDesc() {
            Utilisateur moi = user(USER_A_ID);
            Utilisateur inter1 = user(USER_B_ID);
            Utilisateur inter3 = Utilisateur.builder()
                    .id(3L)
                    .nom("N3")
                    .prenom("P3")
                    .email("u3@test.com")
                    .motdepasse("x")
                    .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                    .build();

            when(messageRepository.findDestinatairesOf(moi)).thenReturn(List.of(inter1));
            when(messageRepository.findExpediteursPour(moi)).thenReturn(List.of(inter1, inter3));

            LocalDateTime t1 = LocalDateTime.of(2025, 6, 1, 10, 0);
            LocalDateTime t2 = LocalDateTime.of(2025, 6, 2, 10, 0);
            Message dernierVers1 = Message.builder()
                    .id(10L)
                    .expediteur(moi)
                    .destinataire(inter1)
                    .contenu("m1")
                    .dateEnvoi(t1)
                    .build();
            Message dernierVers3 = Message.builder()
                    .id(11L)
                    .expediteur(inter3)
                    .destinataire(moi)
                    .contenu("m2")
                    .dateEnvoi(t2)
                    .build();

            Pageable pageable = PageRequest.of(0, 1);
            when(messageRepository.findLastMessageBetweenUsers(moi, inter1, pageable))
                    .thenReturn(List.of(dernierVers1));
            when(messageRepository.findLastMessageBetweenUsers(moi, inter3, pageable))
                    .thenReturn(List.of(dernierVers3));

            List<Message> result = messageService.getDerniersMessagesAvecChaqueUtilisateur(moi);

            assertThat(result).containsExactly(dernierVers3, dernierVers1);
        }

        @Test
        void filtreMessagesSansExpediteurOuDestinataire() {
            Utilisateur moi = user(USER_A_ID);
            Utilisateur inter = user(USER_B_ID);
            when(messageRepository.findDestinatairesOf(moi)).thenReturn(List.of(inter));
            when(messageRepository.findExpediteursPour(moi)).thenReturn(List.of());

            Message incomplet = Message.builder()
                    .id(99L)
                    .expediteur(null)
                    .destinataire(inter)
                    .contenu("x")
                    .dateEnvoi(LocalDateTime.now())
                    .build();
            Pageable pageable = PageRequest.of(0, 1);
            when(messageRepository.findLastMessageBetweenUsers(moi, inter, pageable))
                    .thenReturn(List.of(incomplet));

            assertThat(messageService.getDerniersMessagesAvecChaqueUtilisateur(moi)).isEmpty();
        }
    }
}

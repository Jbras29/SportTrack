package com.jocf.sporttrack.config;

import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.MessageService;
import com.jocf.sporttrack.service.NotificationService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SidebarBadgeControllerAdviceTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private MessageService messageService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private SidebarBadgeControllerAdvice advice;

    @Test
    void ajouterBadgesSidebar_retourneZeroSansUtilisateurEnSession() {
        Model model = new ExtendedModelMap();

        advice.ajouterBadgesSidebar(new MockHttpSession(), model);

        assertThat(model.getAttribute("badgeNotificationsNonLues")).isEqualTo(0L);
        assertThat(model.getAttribute("badgeMessagesNonLus")).isEqualTo(0L);
    }

    @Test
    void ajouterBadgesSidebar_alimenteLesCompteursQuandUtilisateurPresent() {
        Utilisateur utilisateur = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .amis(new ArrayList<>())
                .build();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.UTILISATEUR_ID, 1L);
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(utilisateur));
        when(notificationService.compterNotificationsNonLues(1L)).thenReturn(3L);
        when(messageService.compterMessagesNonLus(utilisateur)).thenReturn(4L);

        Model model = new ExtendedModelMap();
        advice.ajouterBadgesSidebar(session, model);

        assertThat(model.getAttribute("badgeNotificationsNonLues")).isEqualTo(3L);
        assertThat(model.getAttribute("badgeMessagesNonLus")).isEqualTo(4L);
    }
}

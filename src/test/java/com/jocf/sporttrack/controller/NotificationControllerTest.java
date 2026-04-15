package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.NotificationItem;
import com.jocf.sporttrack.enumeration.NotificationType;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.NotificationService;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void notifications_redirigeLoginSansSession() {
        String view = controller.notifications(new MockHttpSession(), new ExtendedModelMap());

        assertThat(view).isEqualTo("redirect:/login");
    }

    @Test
    void notifications_remplitModeleEtMetAJourDerniereConsultation() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(SessionKeys.UTILISATEUR, new SessionUtilisateur(1L, "jane@test.com", "Doe", "Jane"));
        Utilisateur user = Utilisateur.builder()
                .id(1L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .derniereConsultationNotifications(LocalDateTime.of(2026, 4, 1, 10, 0))
                .build();
        NotificationItem item = new NotificationItem(
                NotificationType.REACTION, "titre", "detail", LocalDateTime.now(), "/home", 1L, null, true);
        when(utilisateurService.trouverParId(1L)).thenReturn(Optional.of(user));
        when(notificationService.listerPourUtilisateur(1L, user.getDerniereConsultationNotifications()))
                .thenReturn(List.of(item));
        Model model = new ExtendedModelMap();

        String view = controller.notifications(session, model);

        assertThat(view).isEqualTo("notifications/list");
        assertThat(model.getAttribute("notifications")).isEqualTo(List.of(item));
        verify(utilisateurService).enregistrerDerniereConsultationNotifications(1L);
    }
}

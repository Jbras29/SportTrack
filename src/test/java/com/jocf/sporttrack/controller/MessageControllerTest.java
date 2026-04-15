package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.MessageService;
import com.jocf.sporttrack.service.UtilisateurService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.servlet.view.UrlBasedViewResolver;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MessageControllerTest {

    private static final Long USER_ID = 1L;
    private static final Long AMI_ID = 2L;

    @Mock
    private MessageService messageService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private MessageController messageController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(messageController)
                .setViewResolvers(noopViewResolver())
                .build();
    }

    /**
     * Thymeleaf non chargé : HTML via une vue vide ; les noms {@code redirect:…} deviennent une vraie redirection HTTP.
     */
    private static ViewResolver noopViewResolver() {
        return (viewName, locale) -> {
            if (viewName != null && viewName.startsWith(UrlBasedViewResolver.REDIRECT_URL_PREFIX)) {
                return new RedirectView(
                        viewName.substring(UrlBasedViewResolver.REDIRECT_URL_PREFIX.length()));
            }
            return new View() {
                @Override
                public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) {
                    response.setContentType("text/html;charset=UTF-8");
                }

                @Override
                public String getContentType() {
                    return "text/html";
                }
            };
        };
    }

    private Utilisateur utilisateur(long id) {
        return Utilisateur.builder()
                .id(id)
                .nom("Nom")
                .prenom("Prenom")
                .email("u" + id + "@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
    }

    private MockHttpSession sessionConnectee() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("utilisateurId", USER_ID);
        return session;
    }

    @Test
    void afficherMessages_sansSession_redirigeVersLogin() throws Exception {
        mockMvc.perform(get("/messages"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void afficherMessages_connecte_chargeListe() throws Exception {
        Utilisateur u = utilisateur(USER_ID);
        when(utilisateurService.findByIdWithAmis(USER_ID)).thenReturn(u);
        when(utilisateurService.listerAmisTries(u)).thenReturn(List.of());
        when(messageService.getDerniersMessagesAvecChaqueUtilisateur(u)).thenReturn(List.of());
        when(messageService.compterMessagesNonLus(u)).thenReturn(0L);

        mockMvc.perform(get("/messages").session(sessionConnectee()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("utilisateur", u))
                .andExpect(model().attribute("openDestinataireId", (Object) null));

        verify(messageService, never()).getConversationEtMarquerRecusCommeLus(any(), any());
    }

    @Test
    void afficherMessages_avecOpen_chargeConversation() throws Exception {
        Utilisateur connecte = utilisateur(USER_ID);
        Utilisateur ami = utilisateur(AMI_ID);
        when(utilisateurService.findByIdWithAmis(USER_ID)).thenReturn(connecte);
        when(utilisateurService.listerAmisTries(connecte)).thenReturn(List.of());
        when(messageService.getDerniersMessagesAvecChaqueUtilisateur(connecte)).thenReturn(List.of());
        when(messageService.compterMessagesNonLus(connecte)).thenReturn(0L);
        when(utilisateurService.findById(AMI_ID)).thenReturn(ami);
        when(messageService.getConversationEtMarquerRecusCommeLus(connecte, ami)).thenReturn(List.of());

        mockMvc.perform(get("/messages").param("open", String.valueOf(AMI_ID)).session(sessionConnectee()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("destinataire", ami))
                .andExpect(model().attribute("openDestinataireId", AMI_ID));

        verify(messageService).getConversationEtMarquerRecusCommeLus(connecte, ami);
    }

    @Test
    void conversationPanelFragment_sansSession_retourne401() throws Exception {
        mockMvc.perform(get("/messages/api/conversation-panel/{id}", AMI_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conversationPanelFragment_connecte_rendVueEtModele() throws Exception {
        Utilisateur connecte = utilisateur(USER_ID);
        Utilisateur ami = utilisateur(AMI_ID);
        when(utilisateurService.findById(USER_ID)).thenReturn(connecte);
        when(utilisateurService.findById(AMI_ID)).thenReturn(ami);
        when(messageService.getConversationEtMarquerRecusCommeLus(connecte, ami)).thenReturn(List.of());

        mockMvc.perform(get("/messages/api/conversation-panel/{id}", AMI_ID).session(sessionConnectee()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("destinataire", ami))
                .andExpect(model().attribute("utilisateur", connecte));
    }

    @Test
    void afficherConversation_redirigeAvecOpen() throws Exception {
        mockMvc.perform(get("/messages/conversation/{id}", AMI_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages?open=" + AMI_ID));
    }

    @Test
    void envoyerMessage_sansSession_redirigeLogin() throws Exception {
        mockMvc.perform(post("/messages/envoyer")
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", "Salut"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void envoyerMessage_contenuVide_redirigeAvecErreur() throws Exception {
        mockMvc.perform(post("/messages/envoyer")
                        .session(sessionConnectee())
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", "   "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages?open=" + AMI_ID));

        verify(messageService, never()).envoyerMessage(any(), any(), any());
    }

    @Test
    void envoyerMessage_succes_redirige() throws Exception {
        Utilisateur expediteur = utilisateur(USER_ID);
        Utilisateur destinataire = utilisateur(AMI_ID);
        when(utilisateurService.findById(USER_ID)).thenReturn(expediteur);
        when(utilisateurService.findById(AMI_ID)).thenReturn(destinataire);

        mockMvc.perform(post("/messages/envoyer")
                        .session(sessionConnectee())
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", " Bonjour "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages?open=" + AMI_ID));

        verify(messageService).envoyerMessage(expediteur, destinataire, " Bonjour ");
    }

    @Test
    void envoyerMessage_exception_redirigeAvecErreur() throws Exception {
        Utilisateur expediteur = utilisateur(USER_ID);
        Utilisateur destinataire = utilisateur(AMI_ID);
        when(utilisateurService.findById(USER_ID)).thenReturn(expediteur);
        when(utilisateurService.findById(AMI_ID)).thenReturn(destinataire);
        doThrow(new RuntimeException("échec")).when(messageService)
                .envoyerMessage(expediteur, destinataire, "x");

        mockMvc.perform(post("/messages/envoyer")
                        .session(sessionConnectee())
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", "x"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/messages?open=" + AMI_ID));
    }

    @Test
    void envoyerMessageAjax_sansSession_retourne401() throws Exception {
        mockMvc.perform(post("/messages/api/envoyer")
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", "a"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void envoyerMessageAjax_contenuVide_retourne400() throws Exception {
        mockMvc.perform(post("/messages/api/envoyer")
                        .session(sessionConnectee())
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", " "))
                .andExpect(status().isBadRequest());

        verify(messageService, never()).envoyerMessage(any(), any(), any());
    }

    @Test
    void envoyerMessageAjax_succes_retourne200() throws Exception {
        Utilisateur expediteur = utilisateur(USER_ID);
        Utilisateur destinataire = utilisateur(AMI_ID);
        when(utilisateurService.findById(USER_ID)).thenReturn(expediteur);
        when(utilisateurService.findById(AMI_ID)).thenReturn(destinataire);

        mockMvc.perform(post("/messages/api/envoyer")
                        .session(sessionConnectee())
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", "ok"))
                .andExpect(status().isOk());

        verify(messageService).envoyerMessage(expediteur, destinataire, "ok");
    }

    @Test
    void envoyerMessageAjax_exception_retourne500() throws Exception {
        Utilisateur expediteur = utilisateur(USER_ID);
        Utilisateur destinataire = utilisateur(AMI_ID);
        when(utilisateurService.findById(USER_ID)).thenReturn(expediteur);
        when(utilisateurService.findById(AMI_ID)).thenReturn(destinataire);
        doThrow(new RuntimeException("pan")).when(messageService)
                .envoyerMessage(expediteur, destinataire, "z");

        mockMvc.perform(post("/messages/api/envoyer")
                        .session(sessionConnectee())
                        .param("destinataireId", String.valueOf(AMI_ID))
                        .param("contenu", "z"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void compterMessagesNonLus_sansSession_retourne401() throws Exception {
        mockMvc.perform(get("/messages/api/non-lus"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void compterMessagesNonLus_connecte_retourneNombre() throws Exception {
        Utilisateur u = utilisateur(USER_ID);
        when(utilisateurService.findById(USER_ID)).thenReturn(u);
        when(messageService.compterMessagesNonLus(u)).thenReturn(7L);

        mockMvc.perform(get("/messages/api/non-lus").session(sessionConnectee()))
                .andExpect(status().isOk())
                .andExpect(content().string("7"));
    }
}

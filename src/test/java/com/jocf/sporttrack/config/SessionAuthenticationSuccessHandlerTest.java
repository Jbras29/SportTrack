package com.jocf.sporttrack.config;

import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionAuthenticationSuccessHandlerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private SessionAuthenticationSuccessHandler handler;

    @Test
    void onAuthenticationSuccess_redirigeAdminVersHomeAdmin() throws IOException, ServletException {
        Utilisateur admin = Utilisateur.builder()
                .id(1L)
                .nom("Admin")
                .prenom("Super")
                .email("admin@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.ADMIN)
                .build();
        when(utilisateurService.trouverParEmail("admin@test.com")).thenReturn(admin);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                request,
                response,
                new UsernamePasswordAuthenticationToken(
                        "admin@test.com", "x", java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));

        assertThat(response.getRedirectedUrl()).isEqualTo("/homeAdmin");
        assertThat(request.getSession().getAttribute(SessionKeys.UTILISATEUR_ID)).isEqualTo(1L);
        assertThat(request.getSession().getAttribute(SessionKeys.UTILISATEUR)).isNotNull();
    }

    @Test
    void onAuthenticationSuccess_redirigeUtilisateurStandardVersHome() throws IOException, ServletException {
        Utilisateur user = Utilisateur.builder()
                .id(2L)
                .nom("Doe")
                .prenom("Jane")
                .email("jane@test.com")
                .motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
        when(utilisateurService.trouverParEmail("jane@test.com")).thenReturn(user);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(
                new MockHttpServletRequest(),
                response,
                new UsernamePasswordAuthenticationToken(
                        "jane@test.com", "x", java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(response.getRedirectedUrl()).isEqualTo("/home");
    }
}

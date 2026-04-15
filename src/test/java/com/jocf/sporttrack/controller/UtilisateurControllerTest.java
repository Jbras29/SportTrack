package com.jocf.sporttrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jocf.sporttrack.dto.ModifierUtilisateurRequest;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UtilisateurControllerTest {

    private static final Long USER_ID = 1L;
    private static final String EMAIL = "jean@test.com";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private UtilisateurController utilisateurController;

    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings("removal")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(utilisateurController)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private Utilisateur utilisateurExemple() {
        return Utilisateur.builder()
                .id(USER_ID)
                .nom("Dupont")
                .prenom("Jean")
                .email(EMAIL)
                .motdepasse("hash")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                .build();
    }

    @Test
    void getAllUtilisateurs_retourne200() throws Exception {
        List<Utilisateur> liste = List.of(utilisateurExemple());
        when(utilisateurService.recupererTousLesUtilisateurs()).thenReturn(liste);

        mockMvc.perform(get("/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value(EMAIL));
    }

    @Nested
    class GetCurrentUser {

        @Test
        void sansAuthentication_retourne401() throws Exception {
            mockMvc.perform(get("/utilisateurs/me"))
                    .andExpect(status().isUnauthorized());
            verify(utilisateurService, never()).trouverParEmail(any());
        }

        @Test
        void authentifie_retourneUtilisateur() throws Exception {
            Utilisateur u = utilisateurExemple();
            UserDetails principal = User.withUsername(EMAIL).password("x").roles("USER").build();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, principal.getPassword(), principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            when(utilisateurService.trouverParEmail(EMAIL)).thenReturn(u);

            mockMvc.perform(get("/utilisateurs/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL));
        }

        @Test
        void authentificationNonValidee_retourne401() throws Exception {
            UserDetails principal = User.withUsername(EMAIL).password("x").roles("USER").build();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, principal.getPassword(), principal.getAuthorities());
            auth.setAuthenticated(false);
            SecurityContextHolder.getContext().setAuthentication(auth);

            mockMvc.perform(get("/utilisateurs/me"))
                    .andExpect(status().isUnauthorized());
            verify(utilisateurService, never()).trouverParEmail(any());
        }

        @Test
        void authentifie_maisErreurService_retourne401() throws Exception {
            UserDetails principal = User.withUsername(EMAIL).password("x").roles("USER").build();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, principal.getPassword(), principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            when(utilisateurService.trouverParEmail(EMAIL)).thenThrow(new RuntimeException("db"));

            mockMvc.perform(get("/utilisateurs/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class Login {

        @Test
        void succes_retourne200() throws Exception {
            mockMvc.perform(post("/utilisateurs/auth")
                            .param("email", EMAIL)
                            .param("motDePasse", "secret"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").value("Connexion réussie"));
            verify(utilisateurService).connecter(EMAIL, "secret");
        }

        @Test
        void echec_retourne401() throws Exception {
            doThrow(new RuntimeException("bad")).when(utilisateurService).connecter(EMAIL, "wrong");

            mockMvc.perform(post("/utilisateurs/auth")
                            .param("email", EMAIL)
                            .param("motDePasse", "wrong"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$").value("Email ou mot de passe incorrect"));
        }
    }

    @Nested
    class GetById {

        @Test
        void trouve_retourne200() throws Exception {
            Utilisateur u = utilisateurExemple();
            when(utilisateurService.trouverParId(USER_ID)).thenReturn(Optional.of(u));

            mockMvc.perform(get("/utilisateurs/{id}", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(USER_ID));
        }

        @Test
        void absent_retourne404() throws Exception {
            when(utilisateurService.trouverParId(USER_ID)).thenReturn(Optional.empty());

            mockMvc.perform(get("/utilisateurs/{id}", USER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Update {

        @Test
        void succes_retourne200() throws Exception {
            Utilisateur updated = utilisateurExemple();
            ModifierUtilisateurRequest req = new ModifierUtilisateurRequest();
            req.setNom("Nouveau");
            req.setPrenom("Jean");
            req.setEmail(EMAIL);
            when(utilisateurService.modifierUtilisateur(eq(USER_ID), any(ModifierUtilisateurRequest.class)))
                    .thenReturn(updated);

            mockMvc.perform(put("/utilisateurs/{id}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(EMAIL));
        }

        @Test
        void utilisateurInconnu_retourne404() throws Exception {
            ModifierUtilisateurRequest req = new ModifierUtilisateurRequest();
            req.setEmail(EMAIL);
            when(utilisateurService.modifierUtilisateur(eq(USER_ID), any(ModifierUtilisateurRequest.class)))
                    .thenThrow(new IllegalArgumentException("Utilisateur introuvable : " + USER_ID));

            mockMvc.perform(put("/utilisateurs/{id}", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Delete {

        @Test
        void succes_retourne204() throws Exception {
            mockMvc.perform(delete("/utilisateurs/{id}", USER_ID))
                    .andExpect(status().isNoContent());
            verify(utilisateurService).supprimerUtilisateur(USER_ID);
        }

        @Test
        void utilisateurInconnu_retourne404() throws Exception {
            doThrow(new IllegalArgumentException("introuvable")).when(utilisateurService).supprimerUtilisateur(USER_ID);

            mockMvc.perform(delete("/utilisateurs/{id}", USER_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class MesAmis {

        @Test
        void principalUserDetails_retourneListe() throws Exception {
            UserDetails principal = User.withUsername(EMAIL).password("x").roles("USER").build();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, principal.getPassword(), principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Utilisateur self = utilisateurExemple();
            Utilisateur ami = Utilisateur.builder()
                    .id(2L)
                    .nom("Martin")
                    .prenom("Paul")
                    .email("paul@test.com")
                    .motdepasse("h")
                    .typeUtilisateur(TypeUtilisateur.UTILISATEUR)
                    .build();
            self.setAmis(new ArrayList<>(List.of(ami)));
            when(utilisateurService.trouverParEmail(EMAIL)).thenReturn(self);

            mockMvc.perform(get("/utilisateurs/me/amis"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(2))
                    .andExpect(jsonPath("$[0].nom").value("Martin"));
        }

        @Test
        void principalString_utiliseToStringCommeEmail() throws Exception {
            String emailDirect = "string@test.com";
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    emailDirect,
                    "n/a",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);

            Utilisateur self = utilisateurExemple();
            self.setEmail(emailDirect);
            self.setAmis(new ArrayList<>());
            when(utilisateurService.trouverParEmail(emailDirect)).thenReturn(self);

            mockMvc.perform(get("/utilisateurs/me/amis"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        void utilisateurInconnu_retourne401() throws Exception {
            UserDetails principal = User.withUsername(EMAIL).password("x").roles("USER").build();
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, principal.getPassword(), principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            when(utilisateurService.trouverParEmail(EMAIL)).thenThrow(new UsernameNotFoundException("no"));

            mockMvc.perform(get("/utilisateurs/me/amis"))
                    .andExpect(status().isUnauthorized());
        }
    }
}

package com.jocf.sporttrack.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jocf.sporttrack.dto.CreerEvenementRequest;
import com.jocf.sporttrack.dto.ModifierEvenementRequest;
import com.jocf.sporttrack.model.Evenement;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.EvenementService;
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
import static org.mockito.ArgumentMatchers.*;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EvenementControllerTest {

    private static final String EMAIL_MOCK = "test@test.com";
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private EvenementService evenementService;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private EvenementController evenementController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // Initialisation de MockMvc en mode Standalone comme ton exemple
        mockMvc = MockMvcBuilders.standaloneSetup(evenementController)
                .build();
    }

    @BeforeEach
    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /** * Simule un utilisateur connecté dans le SecurityContext
     */
    private void mockAuthentification(String email) {
        UserDetails principal = User.withUsername(email).password("pass").authorities(new ArrayList<>()).build();
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private Utilisateur utilisateurExemple(Long id, String email) {
        return Utilisateur.builder().id(id).email(email).build();
    }

    @Nested
    class VuesThymeleaf {

        @Test
        void afficherPageEvenements_retourneVue() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(utilisateurExemple(1L, EMAIL_MOCK));
            when(evenementService.obtenirTousLesEvenements()).thenReturn(new ArrayList<>());

            mockMvc.perform(get("/evenements"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("evenement/evenement"))
                    .andExpect(model().attributeExists("user", "evenements"));
        }

        @Test
        void afficherPageEvenements_PrincipalEstUneString_CouvertureTotale() throws Exception {
            // GIVEN : On simule un principal qui est une simple String (pas un UserDetails)
            String emailDirect = "simple-string@test.com";

            // On crée une authentification où le principal est directement l'email
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            emailDirect,
                            "n/a",
                            new java.util.ArrayList<>()
                    );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            // Mock du service pour cet email spécifique
            Utilisateur mockUser = Utilisateur.builder().id(1L).email(emailDirect).build();
            when(utilisateurService.trouverParEmail(emailDirect)).thenReturn(mockUser);
            when(evenementService.obtenirTousLesEvenements()).thenReturn(new java.util.ArrayList<>());

            // WHEN & THEN
            mockMvc.perform(get("/evenements"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("evenement/evenement"));

            verify(utilisateurService).trouverParEmail(emailDirect);
        }

        @Test
        void afficherPageCreerEvenement_Succes() throws Exception {
            // GIVEN : Simuler un utilisateur connecté et son comportement
            String email = "test@test.com";

            // 1. Manuellement mettre un utilisateur dans le SecurityContext
            UserDetails principal = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("pass")
                    .authorities(new java.util.ArrayList<>())
                    .build();
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
            );

            // 2. Créer un utilisateur avec une liste d'amis (pour couvrir currentUser.getAmis())
            Utilisateur mockUser = Utilisateur.builder()
                    .id(1L)
                    .email(email)
                    .amis(new java.util.ArrayList<>())
                    .build();

            when(utilisateurService.trouverParEmail(email)).thenReturn(mockUser);

            // WHEN & THEN : Exécuter la requête GET et vérifier le modèle et la vue
            mockMvc.perform(get("/creer-evenement"))
                    .andExpect(status().isOk()) //
                    .andExpect(view().name("evenement/creer-evenement")) //
                    .andExpect(model().attribute("user", mockUser))
                    .andExpect(model().attribute("amis", mockUser.getAmis()));

            // 3. Vérifier que le service a bien été sollicité

            verify(utilisateurService).trouverParEmail(email);
        }

        @Test
        void afficherPageCreerEvenement_PrincipalEstUneString_CouvertureTotale() throws Exception {
            // GIVEN : On simule un principal qui est une simple String (pas un UserDetails)
            String emailDirect = "test-string@test.com";

            // On crée une authentification manuelle où le "principal" est juste le String de l'email
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            emailDirect,
                            null,
                            new java.util.ArrayList<>()
                    );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            // Mock du service pour cet utilisateur spécifique
            Utilisateur mockUser = Utilisateur.builder()
                    .id(1L)
                    .email(emailDirect)
                    .amis(new java.util.ArrayList<>())
                    .build();

            when(utilisateurService.trouverParEmail(emailDirect)).thenReturn(mockUser);

            // WHEN & THEN : Exécuter la requête
            mockMvc.perform(get("/creer-evenement"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("evenement/creer-evenement"))
                    .andExpect(model().attribute("user", mockUser));

            // On vérifie que le code a bien utilisé principal.toString()
            verify(utilisateurService).trouverParEmail(emailDirect);
        }

        @Test
        void afficherDetail_retourneVue() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            Utilisateur current = utilisateurExemple(1L, EMAIL_MOCK);
            Evenement ev = Evenement.builder().id(10L).organisateur(current).participants(new ArrayList<>()).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(current);

            mockMvc.perform(get("/evenements/10"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("evenement/detail"))
                    .andExpect(model().attribute("isOrganisateur", true));
        }


        @Test
        void afficherDetailEvenement_CouvertureEmailString() throws Exception {
            // GIVEN : Simuler un principal qui est une simple chaîne de caractères
            String emailDirect = "simple-user@test.com";
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    emailDirect, null, new ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Utilisateur mockUser = Utilisateur.builder().id(1L).email(emailDirect).build();
            Evenement mockEv = Evenement.builder().id(10L).organisateur(mockUser).participants(new ArrayList<>()).build();

            when(evenementService.trouverParId(10L)).thenReturn(mockEv);
            when(utilisateurService.trouverParEmail(emailDirect)).thenReturn(mockUser);

            // WHEN & THEN
            mockMvc.perform(get("/evenements/10"))
                    .andExpect(status().isOk());

            // Vérifier que c'est bien l'email String qui a été utilisé
            verify(utilisateurService).trouverParEmail(emailDirect);
        }

        @Test
        void afficherDetailEvenement_CouvertureLambdaParticipants() throws Exception {
            // GIVEN : Utilisateur connecté classique (UserDetails)
            mockAuthentification(EMAIL_MOCK);
            Utilisateur currentUser = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();

            // Créer un participant pour déclencher le Stream
            Utilisateur participant = Utilisateur.builder().id(2L).nom("Ami").build();
            List<Utilisateur> participants = new ArrayList<>();
            participants.add(participant);

            Evenement evenement = Evenement.builder()
                    .id(10L)
                    .organisateur(currentUser)
                    .participants(participants)
                    .build();

            when(evenementService.trouverParId(10L)).thenReturn(evenement);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(currentUser);

            // Mock de la méthode appelée dans le lambda
            when(utilisateurService.peutAfficherIdentiteVers(any(Utilisateur.class), eq(1L)))
                    .thenReturn(true);

            // WHEN & THEN
            mockMvc.perform(get("/evenements/10"))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeExists("afficherNomParticipant"));

            // Vérifier que le lambda a bien exécuté l'appel au service
            verify(utilisateurService).peutAfficherIdentiteVers(participant, 1L);
        }

        @Test
        void afficherDetailEvenement_CouvertureMergeFunction() throws Exception {
            // GIVEN : Simuler un utilisateur connecté
            mockAuthentification(EMAIL_MOCK);
            Utilisateur currentUser = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();

            // IMPORTANT : Créer deux participants avec le MÊME ID pour déclencher le merge (a, b) -> a
            Utilisateur p1 = Utilisateur.builder().id(2L).nom("Ami 1").build();
            Utilisateur p2 = Utilisateur.builder().id(2L).nom("Ami 2").build(); // Même ID !

            List<Utilisateur> participants = new ArrayList<>();
            participants.add(p1);
            participants.add(p2);

            Evenement ev = Evenement.builder()
                    .id(10L)
                    .organisateur(currentUser)
                    .participants(participants)
                    .build();

            // Mock des services
            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(currentUser);
            when(utilisateurService.peutAfficherIdentiteVers(any(), anyLong())).thenReturn(true);

            // WHEN & THEN
            mockMvc.perform(get("/evenements/10"))
                    .andExpect(status().isOk());

            verify(utilisateurService).peutAfficherIdentiteVers(p1, 1L);
        }
    }

    @Nested
    class ApiRestActions {

        @Test
        void getAllEvenements_Succes() throws Exception {
            // GIVEN : Préparer une liste d'événements factices
            List<Evenement> mockListe = new java.util.ArrayList<>();
            mockListe.add(Evenement.builder().id(1L).nom("Événement 1").build());
            mockListe.add(Evenement.builder().id(2L).nom("Événement 2").build());

            // Configurer le mock du service
            when(evenementService.obtenirTousLesEvenements()).thenReturn(mockListe);

            // WHEN & THEN : Exécuter la requête API et vérifier le JSON
            mockMvc.perform(get("/api/evenements")
                            .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk()) // 验证状态码 200
                    .andExpect(jsonPath("$.length()").value(2)) // 验证返回的列表长度为 2
                    .andExpect(jsonPath("$[0].nom").value("Événement 1")) // 验证第一个元素的名称
                    .andExpect(jsonPath("$[1].id").value(2));

            // Vérifier que le service a bien été appelé
            verify(evenementService).obtenirTousLesEvenements();
        }

        @Test
        void apiCreerEvenement_CoverageUserDetails() throws Exception {
            // GIVEN : Simuler un Principal de type UserDetails
            String email = "user@test.com";
            UserDetails principal = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("pass")
                    .authorities(new java.util.ArrayList<>())
                    .build();

            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            // Mock des services
            Utilisateur mockUser = Utilisateur.builder().id(1L).email(email).build();
            when(utilisateurService.trouverParEmail(email)).thenReturn(mockUser);

            CreerEvenementRequest req = new CreerEvenementRequest("Foot", "Desc", java.time.LocalDateTime.now(), new java.util.ArrayList<>());
            when(evenementService.creerEvenement(anyLong(), any())).thenReturn(new Evenement());

            // WHEN & THEN
            mockMvc.perform(post("/api/evenements/creer")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            // Vérification du passage dans le premier bloc du IF
            verify(utilisateurService).trouverParEmail(email);
        }


        @Test
        void apiCreerEvenement_CoveragePrincipalString() throws Exception {
            // GIVEN : Simuler un Principal qui est une simple String (pas UserDetails)
            String emailStr = "string-user@test.com";

            // On passe directement la String comme principal
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(emailStr, null, new java.util.ArrayList<>());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            Utilisateur mockUser = Utilisateur.builder().id(1L).email(emailStr).build();
            when(utilisateurService.trouverParEmail(emailStr)).thenReturn(mockUser);

            CreerEvenementRequest req = new CreerEvenementRequest("Basket", "Desc", java.time.LocalDateTime.now(), new java.util.ArrayList<>());
            when(evenementService.creerEvenement(anyLong(), any())).thenReturn(new Evenement());


            mockMvc.perform(post("/api/evenements/creer")
                            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());

            verify(utilisateurService).trouverParEmail(emailStr);
        }

        @Test
        void creerEvenement_retourne201() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            Utilisateur current = utilisateurExemple(1L, EMAIL_MOCK);
            CreerEvenementRequest req = new CreerEvenementRequest("Run", "Desc", LocalDateTime.now(), new ArrayList<>());

            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(current);
            when(evenementService.creerEvenement(anyLong(), any())).thenReturn(new Evenement());

            mockMvc.perform(post("/api/evenements/creer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        void rejoindreEvenement_succes() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(utilisateurExemple(1L, EMAIL_MOCK));

            mockMvc.perform(post("/api/evenements/10/rejoindre"))
                    .andExpect(status().isOk());
            verify(evenementService).rejoindreEvenement(eq(10L), any());
        }

        @Test
        void rejoindreEvenement_erreur_retourne400() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(utilisateurExemple(1L, EMAIL_MOCK));
            when(evenementService.rejoindreEvenement(anyLong(), any())).thenThrow(new RuntimeException("Déjà participant"));

            mockMvc.perform(post("/api/evenements/10/rejoindre"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("Déjà participant"));
        }

        @Test
        void rejoindreEvenement_CoverageUserDetails() throws Exception {
            String email = "user@test.com";
            UserDetails principal = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("p")
                    .authorities(new java.util.ArrayList<>())
                    .build();

            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            when(utilisateurService.trouverParEmail(email)).thenReturn(Utilisateur.builder().id(1L).build());

            mockMvc.perform(post("/api/evenements/10/rejoindre"))
                    .andExpect(status().isOk());

            verify(utilisateurService).trouverParEmail(email);
        }

        @Test
        void rejoindreEvenement_CoveragePrincipalString() throws Exception {

            String emailStr = "simple-string@test.com";

            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(emailStr, null, new java.util.ArrayList<>());
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            when(utilisateurService.trouverParEmail(emailStr)).thenReturn(Utilisateur.builder().id(1L).build());

            mockMvc.perform(post("/api/evenements/10/rejoindre"))
                    .andExpect(status().isOk());

            verify(utilisateurService).trouverParEmail(emailStr);
        }

        @Test
        void getEvenementsByOrganisateur_Succes() throws Exception {

            Long organisateurId = 1L;
            List<Evenement> mockListe = new java.util.ArrayList<>();
            mockListe.add(Evenement.builder().id(100L).nom("Event de l'organisateur").build());

            // Configurer le mock pour retourner la liste quand on appelle le service avec l'ID 1
            when(evenementService.obtenirEvenementsParOrganisateur(organisateurId)).thenReturn(mockListe);

            mockMvc.perform(get("/api/evenements/organisateur/{organisateurId}", organisateurId)
                            .accept(org.springframework.http.MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(100))
                    .andExpect(jsonPath("$[0].nom").value("Event de l'organisateur"));

            verify(evenementService).obtenirEvenementsParOrganisateur(organisateurId);
        }
    }

    @Nested
    class ApiInteractions {

        /**
         * Test pour quitter un événement.
         */
        @Test
        void quitter_Succes() throws Exception {
            // GIVEN
            mockAuthentification(EMAIL_MOCK);
            Utilisateur current = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(current);

            // WHEN & THEN
            mockMvc.perform(post("/api/evenements/10/quitter"))
                    .andExpect(status().isOk());

            // Vérifier que le service a été appelé avec le bon ID utilisateur
            verify(evenementService).quitterEvenement(eq(10L), eq(1L));
        }

        /**
         * Test pour quitter un événement - Cas 1 : Principal est UserDetails (Branche IF)
         * 测试：用户退出活动 - 场景1：Principal 是 UserDetails 类型（覆盖三元运算符前半部分）
         */
        @Test
        void quitter_CouvertureUserDetails() throws Exception {
            // GIVEN
            String email = "userDetails@test.com";
            UserDetails principal = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("p")
                    .authorities(new java.util.ArrayList<>())
                    .build();

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Utilisateur current = Utilisateur.builder().id(1L).email(email).build();
            when(utilisateurService.trouverParEmail(email)).thenReturn(current);

            // WHEN & THEN
            mockMvc.perform(post("/api/evenements/10/quitter"))
                    .andExpect(status().isOk());

            verify(utilisateurService).trouverParEmail(email);
            verify(evenementService).quitterEvenement(10L, 1L);
        }

        /**
         * Test pour quitter un événement - Cas 2 : Principal est String (Branche ELSE)

         */
        @Test
        void quitter_CouverturePrincipalString() throws Exception {
            // GIVEN
            String emailDirect = "string-user@test.com";
            // On passe directement la String comme principal (pas d'instanceof UserDetails)
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    emailDirect, null, new java.util.ArrayList<>());
            SecurityContextHolder.getContext().setAuthentication(auth);

            Utilisateur current = Utilisateur.builder().id(1L).email(emailDirect).build();
            when(utilisateurService.trouverParEmail(emailDirect)).thenReturn(current);

            // WHEN & THEN
            mockMvc.perform(post("/api/evenements/10/quitter"))
                    .andExpect(status().isOk());

            // Vérification que c'est bien principal.toString() qui a été utilisé
            verify(utilisateurService).trouverParEmail(emailDirect);
            verify(evenementService).quitterEvenement(10L, 1L);
        }

        /**
         * Test pour expulser un participant (Succès - Organisateur).
         */
        @Test
        void kickParticipant_Succes_SiOrganisateur() throws Exception {
            // GIVEN
            mockAuthentification(EMAIL_MOCK);
            Utilisateur org = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();
            Evenement ev = Evenement.builder().id(10L).organisateur(org).build();

            // Mock pour que verifierSiOrganisateur retourne true
            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(org);

            // WHEN & THEN
            mockMvc.perform(delete("/api/evenements/10/participants/2"))
                    .andExpect(status().isOk());

            verify(evenementService).retirerParticipant(10L, 2L);
        }

        /**
         * Test pour expulser un participant (Échec - Non organisateur).
         */
        @Test
        void kickParticipant_Echec_SiNonOrganisateur() throws Exception {
            // GIVEN
            mockAuthentification(EMAIL_MOCK);
            Utilisateur orgReal = Utilisateur.builder().id(99L).build(); // Le vrai chef
            Utilisateur pirate = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build(); // Moi
            Evenement ev = Evenement.builder().id(10L).organisateur(orgReal).build();

            // Mock pour que verifierSiOrganisateur retourne false (1L != 99L)
            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(pirate);

            // WHEN & THEN
            mockMvc.perform(delete("/api/evenements/10/participants/2"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string("Action non autorisée."));
        }


        @Test
        void creerAnnonce_Echec_SiNonOrganisateur() throws Exception {
            mockAuthentification(EMAIL_MOCK);


            Utilisateur pirate = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();

            Utilisateur vraiChef = Utilisateur.builder().id(99L).build();

            Evenement ev = Evenement.builder().id(10L).organisateur(vraiChef).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(pirate);

            Map<String, String> payload = new HashMap<>();
            payload.put("message", "Ceci est un test");


            mockMvc.perform(post("/api/evenements/10/annonces")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string("Non autorisé"));
        }

        /**
         * Test pour supprimer une annonce (Succès - Organisateur).
         */
        @Test
        void supprimerAnnonce_Succes_SiOrganisateur() throws Exception {
            // GIVEN
            mockAuthentification(EMAIL_MOCK);
            Utilisateur org = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();
            Evenement ev = Evenement.builder().id(10L).organisateur(org).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(org);

            // WHEN & THEN
            mockMvc.perform(delete("/api/evenements/10/annonces/50"))
                    .andExpect(status().isOk());

            verify(evenementService).supprimerAnnonce(50L);
        }

        /**
         * Test pour supprimer une annonce (Échec - Non organisateur).
         */
        @Test
        void supprimerAnnonce_Echec_SiNonOrganisateur() throws Exception {
            // GIVEN
            mockAuthentification(EMAIL_MOCK);
            Utilisateur orgReal = Utilisateur.builder().id(99L).build();
            Utilisateur pirate = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();
            Evenement ev = Evenement.builder().id(10L).organisateur(orgReal).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(pirate);

            // WHEN & THEN
            mockMvc.perform(delete("/api/evenements/10/annonces/50"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void supprimerEvenement_Succes_UserDetails() throws Exception {
            // GIVEN : Préparer un utilisateur avec UserDetails
            /* Simulation d'un principal de type UserDetails pour la branche 'if' du ternaire */
            String email = "admin@test.com";
            UserDetails principal = org.springframework.security.core.userdetails.User
                    .withUsername(email).password("p").authorities(new java.util.ArrayList<>()).build();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

            Utilisateur user = Utilisateur.builder().id(1L).email(email).build();
            /* L'organisateur a le même ID que l'utilisateur actuel (1L) */
            Evenement ev = Evenement.builder().id(10L).organisateur(user).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(email)).thenReturn(user);

            // WHEN & THEN
            mockMvc.perform(delete("/api/evenements/10"))
                    .andExpect(status().isOk());

            /* On vérifie que la suppression a bien été appelée car l'ID correspondait */
            verify(evenementService).supprimer(10L);
        }

        /**
         * Test : Principal est une String.
         */
        @Test
        void supprimerEvenement_Succes_StringPrincipal() throws Exception {
            // GIVEN : Simuler un principal qui est une simple chaîne
            /* Branche 'else' du ternaire : le principal n'est pas une instance de UserDetails */
            String emailStr = "simple@test.com";
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(emailStr, null, new java.util.ArrayList<>()));

            Utilisateur user = Utilisateur.builder().id(1L).email(emailStr).build();
            Evenement ev = Evenement.builder().id(10L).organisateur(user).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(emailStr)).thenReturn(user);

            // WHEN & THEN
            mockMvc.perform(delete("/api/evenements/10"))
                    .andExpect(status().isOk());
        }

        /**
         * Test : Échec - Non organisateur (ID différents).
         */
        @Test
        void supprimerEvenement_Echec_NonOrganisateur() throws Exception {
            // GIVEN : L'utilisateur actuel n'est pas celui qui a créé l'événement
            /* Utilisation de ton helper mockAuthentification */
            mockAuthentification(EMAIL_MOCK);

            Utilisateur pirate = Utilisateur.builder().id(1L).email(EMAIL_MOCK).build();
            /* L'organisateur a un ID différent (99L) */
            Utilisateur vraiChef = Utilisateur.builder().id(99L).build();
            Evenement ev = Evenement.builder().id(10L).organisateur(vraiChef).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(pirate);

            // WHEN & THEN
            /* Doit entrer dans le bloc 'if' et retourner Forbidden */
            mockMvc.perform(delete("/api/evenements/10"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string("Seul l'organisateur peut supprimer cet événement."));

            /* Le service de suppression ne doit jamais être appelé */
            verify(evenementService, never()).supprimer(anyLong());
        }


    }




    @Nested
    class SecuriteEtPermissions {

        @Test
        void modifierEvenement_siOrganisateur_retourne200() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            Utilisateur org = utilisateurExemple(1L, EMAIL_MOCK);
            Evenement ev = Evenement.builder().id(10L).organisateur(org).build();
            ModifierEvenementRequest req = new ModifierEvenementRequest("Nouveau","Desc", LocalDateTime.now() );

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(org);

            mockMvc.perform(put("/api/evenements/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk());
        }

        @Test
        void modifierEvenement_siNonOrganisateur_retourne403() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            Utilisateur org = utilisateurExemple(99L, "autre@test.com");
            Utilisateur pirate = utilisateurExemple(1L, EMAIL_MOCK);
            Evenement ev = Evenement.builder().id(10L).organisateur(org).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(pirate);

            mockMvc.perform(put("/api/evenements/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new ModifierEvenementRequest("x", "x",null))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void creerAnnonce_siOrganisateur_retourne200() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            Utilisateur org = utilisateurExemple(1L, EMAIL_MOCK);
            Evenement ev = Evenement.builder().id(10L).organisateur(org).build();
            Map<String, String> payload = new HashMap<>();
            payload.put("message", "Hello");

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(org);

            mockMvc.perform(post("/api/evenements/10/annonces")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());
        }

        @Test
        void supprimerEvenement_siOrganisateur_succes() throws Exception {
            mockAuthentification(EMAIL_MOCK);
            Utilisateur org = utilisateurExemple(1L, EMAIL_MOCK);
            Evenement ev = Evenement.builder().id(10L).organisateur(org).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(EMAIL_MOCK)).thenReturn(org);

            mockMvc.perform(delete("/api/evenements/10"))
                    .andExpect(status().isOk());
            verify(evenementService).supprimer(10L);
        }

        /**
         * Test pour couvrir la branche 'if (principal instanceof UserDetails)'
         * dans la méthode privée verifierSiOrganisateur.
         */
        @Test
        void verifierSiOrganisateur_Couverture_UserDetails() throws Exception {
            // GIVEN
            String email = "user@test.com";
            /* On crée un vrai objet UserDetails */
            UserDetails principal = org.springframework.security.core.userdetails.User
                    .withUsername(email)
                    .password("p")
                    .authorities(new java.util.ArrayList<>())
                    .build();

            /* On l'injecte dans le contexte */
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

            // IMPORTANT : Le service doit retourner un objet pour que la méthode continue
            Utilisateur user = Utilisateur.builder().id(1L).email(email).build();
            Evenement ev = Evenement.builder().id(10L).organisateur(user).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(email)).thenReturn(user);

            // WHEN : On appelle une méthode publique qui utilise le private verifierSiOrganisateur
            Map<String, String> payload = new HashMap<>();
            payload.put("message", "test");

            mockMvc.perform(post("/api/evenements/10/annonces")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            // THEN : La branche 'if (principal instanceof UserDetails)' est maintenant couverte
            verify(utilisateurService).trouverParEmail(email);
        }

        /**
         * Test pour couvrir la branche 'else { email = principal.toString() }'
         * dans la méthode privée verifierSiOrganisateur.
         */
        @Test
        void verifierSiOrganisateur_Couverture_StringPrincipal() throws Exception {
            // GIVEN
            String emailDirect = "simple-string@test.com";

            /* CRUCIAL : On injecte directement une String comme principal, pas un objet UserDetails */
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(emailDirect, null, new java.util.ArrayList<>()));

            // Le service doit retourner des objets valides
            Utilisateur user = Utilisateur.builder().id(1L).email(emailDirect).build();
            Evenement ev = Evenement.builder().id(10L).organisateur(user).build();

            when(evenementService.trouverParId(10L)).thenReturn(ev);
            when(utilisateurService.trouverParEmail(emailDirect)).thenReturn(user);

            // WHEN
            Map<String, String> payload = new HashMap<>();
            payload.put("message", "test string");

            mockMvc.perform(post("/api/evenements/10/annonces")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk());

            // THEN : La branche 'else' est maintenant couverte car principal.toString() a été exécuté
            verify(utilisateurService).trouverParEmail(emailDirect);
        }
    }
}
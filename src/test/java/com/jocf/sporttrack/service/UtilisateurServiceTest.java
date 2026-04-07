package com.jocf.sporttrack.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private Authentication authentication;

    private UtilisateurService utilisateurService;

    @BeforeEach
    void setUp() {
        utilisateurService = new UtilisateurService(
                utilisateurRepository,
                passwordEncoder,
                authenticationConfiguration);
    }

    @Test
    void creerUtilisateurEncodeLeMotdepasseEtAppliqueLesValeursParDefaut() {
        Utilisateur utilisateur = Utilisateur.builder()
                .nom("Dupont")
                .prenom("Jean")
                .username("jdupont")
                .motdepasse("secret")
                .build();

        when(utilisateurRepository.existsByUsername("jdupont")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Utilisateur resultat = utilisateurService.creerUtilisateur(utilisateur);

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(captor.capture());

        Utilisateur sauvegarde = captor.getValue();
        assertEquals("hashed-secret", sauvegarde.getMotdepasse());
        assertEquals(0, sauvegarde.getXp());
        assertEquals(100, sauvegarde.getHp());
        assertEquals("hashed-secret", resultat.getMotdepasse());
        assertEquals(0, resultat.getXp());
        assertEquals(100, resultat.getHp());
    }

    @Test
    void creerUtilisateurRefuseUnUsernameDejaUtilise() {
        Utilisateur utilisateur = Utilisateur.builder()
                .username("jdupont")
                .motdepasse("secret")
                .build();

        when(utilisateurRepository.existsByUsername("jdupont")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> utilisateurService.creerUtilisateur(utilisateur));

        assertEquals("Ce username est deja utilise.", exception.getMessage());
        verify(passwordEncoder, never()).encode(any());
        verify(utilisateurRepository, never()).save(any());
    }

    @Test
    void loadUserByUsernameRetourneUnUserDetailsSpringSecurity() {
        Utilisateur utilisateur = Utilisateur.builder()
                .username("jdupont")
                .motdepasse("hashed-secret")
                .build();

        when(utilisateurRepository.findByUsername("jdupont")).thenReturn(Optional.of(utilisateur));

        UserDetails userDetails = utilisateurService.loadUserByUsername("jdupont");

        assertEquals("jdupont", userDetails.getUsername());
        assertEquals("hashed-secret", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }

    @Test
    void loadUserByUsernameLeveUneExceptionSiUtilisateurIntrouvable() {
        when(utilisateurRepository.findByUsername("inconnu")).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> utilisateurService.loadUserByUsername("inconnu"));
    }

    @Test
    void connecterAuthentifiePuisRetourneLUtilisateur() {
        Utilisateur utilisateur = Utilisateur.builder()
                .id(1L)
                .username("jdupont")
                .motdepasse("hashed-secret")
                .build();

        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getName()).thenReturn("jdupont");
        when(utilisateurRepository.findByUsername("jdupont")).thenReturn(Optional.of(utilisateur));

        Utilisateur resultat = utilisateurService.connecter("jdupont", "secret");

        assertNotNull(resultat);
        assertEquals(1L, resultat.getId());
        assertEquals("jdupont", resultat.getUsername());
    }
}

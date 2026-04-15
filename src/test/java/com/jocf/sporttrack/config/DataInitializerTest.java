package com.jocf.sporttrack.config;

import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private DataInitializer initializer;

    @Test
    void run_neCreePasAdminSIlExisteDeja() {
        when(utilisateurRepository.existsByEmail("admin@dev.com")).thenReturn(true);

        initializer.run();

        verify(utilisateurService, never()).creerUtilisateur(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void run_creeLAdminParDefaut() {
        when(utilisateurRepository.existsByEmail("admin@dev.com")).thenReturn(false);

        initializer.run();

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurService).creerUtilisateur(captor.capture());
        assertThat(captor.getValue().getTypeUtilisateur()).isEqualTo(TypeUtilisateur.ADMIN);
        assertThat(captor.getValue().getEmail()).isEqualTo("admin@dev.com");
    }
}

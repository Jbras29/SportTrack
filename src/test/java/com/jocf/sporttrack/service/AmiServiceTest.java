package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.util.ArrayList;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AmiServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @InjectMocks
    private AmiService service;

    @Test
    void envoyerDemandeAmi_ajouteLeDestinataireAuxDemandesEnvoyees() {
        Utilisateur expediteur = Utilisateur.builder()
                .id(1L).nom("A").prenom("A").email("a@test.com").motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR).demandesAmisEnvoyees(new ArrayList<>()).build();
        Utilisateur destinataire = Utilisateur.builder()
                .id(2L).nom("B").prenom("B").email("b@test.com").motdepasse("x")
                .typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(expediteur));
        when(utilisateurRepository.findById(2L)).thenReturn(Optional.of(destinataire));

        service.envoyerDemandeAmi(1L, 2L);

        assertThat(expediteur.getDemandesAmisEnvoyees()).containsExactly(destinataire);
        verify(utilisateurRepository).save(expediteur);
    }
}

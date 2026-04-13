package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.repository.PrefSportiveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrefSportiveServiceTest {

    private static final Long PREF_ID = 7L;

    @Mock
    private PrefSportiveRepository prefSportiveRepository;

    @InjectMocks
    private PrefSportiveService prefSportiveService;

    @BeforeEach
    void lenientSave() {
        lenient().when(prefSportiveRepository.save(any(PrefSportive.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void recupererToutesLesPrefSportives_delegueAuRepository() {
        PrefSportive p = PrefSportive.builder().id(PREF_ID).nom("Course").build();
        when(prefSportiveRepository.findAll()).thenReturn(List.of(p));

        assertThat(prefSportiveService.recupererToutesLesPrefSportives()).containsExactly(p);
    }

    @Test
    void trouverParId_delegueAuRepository() {
        PrefSportive p = PrefSportive.builder().id(PREF_ID).nom("Natation").build();
        when(prefSportiveRepository.findById(PREF_ID)).thenReturn(Optional.of(p));

        assertThat(prefSportiveService.trouverParId(PREF_ID)).contains(p);
    }

    @Test
    void trouverParNom_delegueAuRepository() {
        PrefSportive p = PrefSportive.builder().id(PREF_ID).nom("Vélo").build();
        when(prefSportiveRepository.findByNom("Vélo")).thenReturn(Optional.of(p));

        assertThat(prefSportiveService.trouverParNom("Vélo")).contains(p);
    }

    @Test
    void creerPrefSportive_sauvegardeAvecNom() {
        PrefSportive cree = prefSportiveService.creerPrefSportive("Escalade");

        assertThat(cree.getNom()).isEqualTo("Escalade");
        verify(prefSportiveRepository).save(any(PrefSportive.class));
    }

    @Nested
    class ModifierPrefSportive {

        @Test
        void introuvable_leveIllegalArgumentException() {
            when(prefSportiveRepository.findById(PREF_ID)).thenReturn(Optional.empty());
            Executable appel = () -> prefSportiveService.modifierPrefSportive(PREF_ID, "X");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("introuvable");
            verify(prefSportiveRepository, never()).save(any());
        }

        @Test
        void ok_metAJourLeNom() {
            PrefSportive enBase = PrefSportive.builder().id(PREF_ID).nom("Ancien").build();
            when(prefSportiveRepository.findById(PREF_ID)).thenReturn(Optional.of(enBase));

            PrefSportive result = prefSportiveService.modifierPrefSportive(PREF_ID, "Nouveau");

            assertThat(result.getNom()).isEqualTo("Nouveau");
            verify(prefSportiveRepository).save(enBase);
        }
    }

    @Nested
    class SupprimerPrefSportive {

        @Test
        void introuvable_leveIllegalArgumentException() {
            when(prefSportiveRepository.existsById(PREF_ID)).thenReturn(false);
            Executable appel = () -> prefSportiveService.supprimerPrefSportive(PREF_ID);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, appel);
            assertThat(ex.getMessage()).contains("introuvable");
            verify(prefSportiveRepository, never()).deleteById(any());
        }

        @Test
        void ok_supprime() {
            when(prefSportiveRepository.existsById(PREF_ID)).thenReturn(true);

            prefSportiveService.supprimerPrefSportive(PREF_ID);

            verify(prefSportiveRepository).deleteById(PREF_ID);
        }
    }
}

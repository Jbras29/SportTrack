package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.service.PrefSportiveService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrefSportiveControllerTest {

    @Mock
    private PrefSportiveService prefSportiveService;

    @InjectMocks
    private PrefSportiveController controller;

    @Test
    void getAllPrefSportives_retourneListe() {
        PrefSportive pref = new PrefSportive(1L, "Course", List.of());
        when(prefSportiveService.recupererToutesLesPrefSportives()).thenReturn(List.of(pref));

        var response = controller.getAllPrefSportives();

        assertThat(response.getBody()).containsExactly(pref);
    }

    @Test
    void getPrefSportiveById_retourne404SiAbsente() {
        when(prefSportiveService.trouverParId(1L)).thenReturn(Optional.empty());

        var response = controller.getPrefSportiveById(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void getPrefSportiveByNom_retourneEntite() {
        PrefSportive pref = new PrefSportive(1L, "Course", List.of());
        when(prefSportiveService.trouverParNom("Course")).thenReturn(Optional.of(pref));

        var response = controller.getPrefSportiveByNom("Course");

        assertThat(response.getBody()).isSameAs(pref);
    }

    @Test
    void createPrefSportive_retourneEntite() {
        PrefSportive pref = new PrefSportive(1L, "Course", List.of());
        when(prefSportiveService.creerPrefSportive("Course")).thenReturn(pref);

        var response = controller.createPrefSportive("Course");

        assertThat(response.getBody()).isSameAs(pref);
    }

    @Test
    void updatePrefSportive_retourne404QuandErreur() {
        when(prefSportiveService.modifierPrefSportive(1L, "Yoga"))
                .thenThrow(new IllegalArgumentException("absente"));

        var response = controller.updatePrefSportive(1L, "Yoga");

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void deletePrefSportive_retourne204QuandSucces() {
        var response = controller.deletePrefSportive(1L);

        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}

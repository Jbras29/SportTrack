package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.service.AnnonceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnnonceControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AnnonceService annonceService;

    @InjectMocks
    private AnnonceController annonceController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(annonceController).build();
    }

    private Annonce buildAnnonce(Long id, String message) {
        return Annonce.builder()
                .id(id)
                .message(message)
                .date(LocalDateTime.of(2026, 4, 14, 10, 0))
                .build();
    }

    @Test
    @DisplayName("GET /api/annonces -> retourne toutes les annonces")
    void shouldReturnAllAnnonces() throws Exception {
        when(annonceService.recupererToutesLesAnnonces())
                .thenReturn(List.of(buildAnnonce(1L, "Annonce 1"), buildAnnonce(2L, "Annonce 2")));

        mockMvc.perform(get("/api/annonces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("Annonce 1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].message").value("Annonce 2"));
    }

    @Test
    @DisplayName("GET /api/annonces/{id} -> retourne une annonce si trouvée")
    void shouldReturnAnnonceById() throws Exception {
        when(annonceService.trouverParId(1L))
                .thenReturn(Optional.of(buildAnnonce(1L, "Annonce trouvée")));

        mockMvc.perform(get("/api/annonces/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").value("Annonce trouvée"));
    }

    @Test
    @DisplayName("GET /api/annonces/{id} -> 404 si annonce introuvable")
    void shouldReturnNotFoundWhenAnnonceByIdDoesNotExist() throws Exception {
        when(annonceService.trouverParId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/annonces/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/annonces/evenement/{evenementId} -> retourne les annonces de l'événement")
    void shouldReturnAnnoncesByEvenement() throws Exception {
        when(annonceService.recupererAnnoncesParEvenement(10L))
                .thenReturn(List.of(buildAnnonce(1L, "Annonce événement")));

        mockMvc.perform(get("/api/annonces/evenement/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("Annonce événement"));
    }

    @Test
    @DisplayName("GET /api/annonces/evenement/{evenementId} -> 404 si événement introuvable")
    void shouldReturnNotFoundForAnnoncesByEvenement() throws Exception {
        when(annonceService.recupererAnnoncesParEvenement(10L))
                .thenThrow(new IllegalArgumentException("Événement introuvable"));

        mockMvc.perform(get("/api/annonces/evenement/10"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/annonces/dashboard/{utilisateurId} -> retourne les annonces du dashboard")
    void shouldReturnDashboardAnnonces() throws Exception {
        when(annonceService.recupererAnnoncesParParticipant(5L))
                .thenReturn(List.of(buildAnnonce(1L, "Annonce dashboard")));

        mockMvc.perform(get("/api/annonces/dashboard/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].message").value("Annonce dashboard"));
    }

    @Test
    @DisplayName("GET /api/annonces/dashboard/{utilisateurId} -> 404 si utilisateur introuvable")
    void shouldReturnNotFoundForDashboardAnnonces() throws Exception {
        when(annonceService.recupererAnnoncesParParticipant(5L))
                .thenThrow(new IllegalArgumentException("Utilisateur introuvable"));

        mockMvc.perform(get("/api/annonces/dashboard/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/annonces -> crée une annonce")
    void shouldCreateAnnonce() throws Exception {
        when(annonceService.creerAnnonce(10L, 20L, "Nouvelle annonce"))
                .thenReturn(buildAnnonce(1L, "Nouvelle annonce"));

        mockMvc.perform(post("/api/annonces")
                        .param("evenementId", "10")
                        .param("organisateurId", "20")
                        .param("message", "Nouvelle annonce"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").value("Nouvelle annonce"));
    }

    @Test
    @DisplayName("POST /api/annonces -> 400 si données invalides")
    void shouldReturnBadRequestWhenCreateAnnonceFails() throws Exception {
        when(annonceService.creerAnnonce(anyLong(), anyLong(), anyString()))
                .thenThrow(new IllegalArgumentException("Données invalides"));

        mockMvc.perform(post("/api/annonces")
                        .param("evenementId", "10")
                        .param("organisateurId", "20")
                        .param("message", "Nouvelle annonce"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/annonces/{id} -> modifie une annonce")
    void shouldUpdateAnnonce() throws Exception {
        when(annonceService.modifierAnnonce(1L, "Message modifié"))
                .thenReturn(buildAnnonce(1L, "Message modifié"));

        mockMvc.perform(put("/api/annonces/1")
                        .param("nouveauMessage", "Message modifié"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.message").value("Message modifié"));
    }

    @Test
    @DisplayName("PUT /api/annonces/{id} -> 404 si annonce introuvable")
    void shouldReturnNotFoundWhenUpdateAnnonceFails() throws Exception {
        when(annonceService.modifierAnnonce(1L, "Message modifié"))
                .thenThrow(new IllegalArgumentException("Annonce introuvable"));

        mockMvc.perform(put("/api/annonces/1")
                        .param("nouveauMessage", "Message modifié"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/annonces/{id} -> supprime une annonce")
    void shouldDeleteAnnonce() throws Exception {
        doNothing().when(annonceService).supprimerAnnonce(1L);

        mockMvc.perform(delete("/api/annonces/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/annonces/{id} -> 404 si annonce introuvable")
    void shouldReturnNotFoundWhenDeleteAnnonceFails() throws Exception {
        doThrow(new IllegalArgumentException("Annonce introuvable"))
                .when(annonceService).supprimerAnnonce(1L);

        mockMvc.perform(delete("/api/annonces/1"))
                .andExpect(status().isNotFound());
    }
}
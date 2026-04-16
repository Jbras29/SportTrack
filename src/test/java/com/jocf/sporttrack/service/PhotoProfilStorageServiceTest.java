package com.jocf.sporttrack.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.test.util.ReflectionTestUtils;

class PhotoProfilStorageServiceTest {

    @TempDir
    Path repertoireTemporaire;

    private PhotoProfilStorageService service;

    @BeforeEach
    void setUp() {
        service = new PhotoProfilStorageService(repertoireTemporaire.toString());
    }

    @Test
    void fichierNull_leveIllegalArgumentException() {
        assertThatThrownBy(() -> service.enregistrerPhotoProfil(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aucun fichier");
    }

    @Test
    void fichierVide_leveIllegalArgumentException() {
        MockMultipartFile vide = new MockMultipartFile("f", "x.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.enregistrerPhotoProfil(vide, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aucun fichier");
    }

    @Test
    void fichierTropVolumineux_leveIllegalArgumentException() {
        byte[] data = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile gros = new MockMultipartFile("f", "x.png", "image/png", data);

        assertThatThrownBy(() -> service.enregistrerPhotoProfil(gros, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2 Mo");
    }

    @Test
    void typeNonImage_leveIllegalArgumentException() {
        MockMultipartFile pdf = new MockMultipartFile("f", "x.pdf", "application/pdf", new byte[] { 1, 2 });

        assertThatThrownBy(() -> service.enregistrerPhotoProfil(pdf, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image");
    }

    @Test
    void typeNull_leveIllegalArgumentException() {
        MockMultipartFile sansType = new MockMultipartFile("f", "x.bin", null, new byte[] { 1 });

        assertThatThrownBy(() -> service.enregistrerPhotoProfil(sansType, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("image");
    }

    @Test
    void ok_ecritSousRepertoireEtRetourneCheminUrl() throws IOException {
        byte[] png = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47 };
        MockMultipartFile fichier =
                new MockMultipartFile("f", "photo.png", "image/png", png);

        String url = service.enregistrerPhotoProfil(fichier, 42L);

        assertThat(url)
                .startsWith("/uploads/profiles/42-")
                .endsWith(".png");
        String nomFichier = url.substring("/uploads/profiles/".length());
        Path cible = repertoireTemporaire.resolve(nomFichier);
        assertThat(Files.exists(cible)).isTrue();
        assertThat(Files.readAllBytes(cible)).isEqualTo(png);
    }

    @Test
    void extensionDepuisContentType_webp() throws IOException {
        MockMultipartFile fichier =
                new MockMultipartFile("f", "p.webp", "image/webp", new byte[] { 1 });

        String url = service.enregistrerPhotoProfil(fichier, 1L);

        assertThat(url).endsWith(".webp");
    }

    @Test
    void extensionDepuisContentType_imageGenerique_utiliseJpg() throws IOException {
        MockMultipartFile fichier =
                new MockMultipartFile("f", "p", "image/avif", new byte[] { 1 });

        String url = service.enregistrerPhotoProfil(fichier, 1L);

        assertThat(url).endsWith(".jpg");
    }

    @Test
    void extensionDepuisContentType_gif() throws IOException {
        MockMultipartFile fichier =
                new MockMultipartFile("f", "p.gif", "image/gif", new byte[] { 1 });

        String url = service.enregistrerPhotoProfil(fichier, 1L);

        assertThat(url).endsWith(".gif");
    }

    @Test
    void extensionDepuisContentType_jpeg() throws IOException {
        MockMultipartFile fichier =
                new MockMultipartFile("f", "p.jpeg", "image/jpeg", new byte[] { 1 });

        String url = service.enregistrerPhotoProfil(fichier, 1L);

        assertThat(url).endsWith(".jpg");
    }

    @Test
    void cheminInvalide_leveIllegalArgumentException() {
        ReflectionTestUtils.setField(
                service,
                "repertoireUpload",
                Path.of(repertoireTemporaire.toString(), "..", "evil"));
        MockMultipartFile fichier = new MockMultipartFile("f", "photo.png", "image/png", new byte[] { 1 });

        assertThatThrownBy(() -> service.enregistrerPhotoProfil(fichier, 42L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chemin de fichier invalide");
    }
}

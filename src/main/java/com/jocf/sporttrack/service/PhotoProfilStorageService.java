package com.jocf.sporttrack.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PhotoProfilStorageService {

    private static final long TAILLE_MAX_OCTETS = 2 * 1024 * 1024;

    private final Path repertoireUpload;

    public PhotoProfilStorageService(
            @Value("${sporttrack.profile-upload-path}") String cheminRepertoireConfiguration) {
        this.repertoireUpload = Paths.get(cheminRepertoireConfiguration).toAbsolutePath().normalize();
    }

    /**
     * Enregistre le fichier et renvoie le chemin URL (ex. /uploads/profiles/…)
     * utilisable dans les pages statiques.
     */
    public String enregistrerPhotoProfil(MultipartFile fichier, Long utilisateurId) throws IOException {
        if (fichier == null || fichier.isEmpty()) {
            throw new IllegalArgumentException("Aucun fichier fourni.");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new IllegalArgumentException("Fichier trop volumineux (max. 2 Mo).");
        }

        String type = fichier.getContentType();
        if (type == null || !type.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image.");
        }

        String extension = extensionDepuisType(type);
        String nomFichier = utilisateurId + "-" + UUID.randomUUID() + extension;

        Files.createDirectories(repertoireUpload);
        Path cible = repertoireUpload.resolve(nomFichier).normalize();
        if (!cible.startsWith(repertoireUpload)) {
            throw new IllegalArgumentException("Chemin de fichier invalide.");
        }

        Files.copy(fichier.getInputStream(), cible, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return "/uploads/profiles/" + nomFichier;
    }

    private static String extensionDepuisType(String contentType) {
        String ct = contentType.toLowerCase(Locale.ROOT);
        if (ct.contains("png")) {
            return ".png";
        }
        if (ct.contains("gif")) {
            return ".gif";
        }
        if (ct.contains("webp")) {
            return ".webp";
        }
        if (ct.contains("jpeg") || ct.contains("jpg")) {
            return ".jpg";
        }
        return ".jpg";
    }
}

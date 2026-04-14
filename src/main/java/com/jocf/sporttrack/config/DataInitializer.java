package com.jocf.sporttrack.config;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;

    public DataInitializer(UtilisateurRepository utilisateurRepository, UtilisateurService utilisateurService) {
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurService = utilisateurService;
    }

    @Override
    public void run(String... args) {
        String emailAdmin = "admin@dev.com";
        String motDePasse = "12345678";

        if (utilisateurRepository.existsByEmail(emailAdmin)) {
            log.info("Administrateur déjà existant — pas de création");
            return;
        }

        Utilisateur admin = Utilisateur.builder()
                .nom("Admin")
                .prenom("Super")
                .email(emailAdmin)
                .motdepasse(motDePasse)
                .typeUtilisateur(TypeUtilisateur.ADMIN)
                .xp(0)
                .hp(100)
                .build();

        utilisateurService.creerUtilisateur(admin);

        log.info("Administrateur par défaut créé — email: {}", emailAdmin);
    }
}
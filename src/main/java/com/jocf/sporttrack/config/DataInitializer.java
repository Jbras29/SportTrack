package com.jocf.sporttrack.config;


import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.model.TypeUtilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UtilisateurService utilisateurService;

    @Override
    public void run(String... args) throws Exception {
        String emailAdmin = "admin@dev.com";
        String motDePasse = "12345678";

        if (!utilisateurService.trouverParEmail(emailAdmin).equals(null)) {
            try {
                utilisateurService.trouverParEmail(emailAdmin);
                System.out.println("ℹ️  Administrateur déjà existant - Pas de création");
            } catch (Exception e) {
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

                System.out.println("╔═══════════════════════════════════════════════════╗");
                System.out.println("║  ✓ ADMINISTRATEUR PAR DÉFAUT CRÉÉ                 ║");
                System.out.println("║  Email    : admin@dev.com                  ║");
                System.out.println("║  Password : 12345678                              ║");
                System.out.println("╚═══════════════════════════════════════════════════╝");
            }
        }
    }
}
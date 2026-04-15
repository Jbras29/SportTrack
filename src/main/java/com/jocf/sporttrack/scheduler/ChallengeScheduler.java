package com.jocf.sporttrack.scheduler;

import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.ChallengeService;
import com.jocf.sporttrack.service.UtilisateurService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.List;

@Component
public class ChallengeScheduler {

    private final ChallengeService challengeService;
    private final UtilisateurService utilisateurService;

    public ChallengeScheduler(ChallengeService challengeService, UtilisateurService utilisateurService) {
        this.challengeService = challengeService;
        this.utilisateurService = utilisateurService;
    }

    /**
     * 🚀 Exécuté tous les jours à minuit pour sanctionner les échecs.
     */
    @Scheduled(cron = "0 1 0 * * *")
    public void verifierLesChallengesTermines() {
        LocalDate hier = LocalDate.now().minusDays(1);

        // 1. Trouver les challenges qui se sont terminés hier
        List<Challenge> challengesFinis = challengeService.trouverChallengesTerminesLe(hier);

        for (Challenge challenge : challengesFinis) {
            // 2. Récupérer le classement final du challenge
            // (Supposons que ton classement contient le score/nombre de validations)
            var classement = challengeService.getClassement(challenge.getId());

            // Calculer la durée totale du challenge pour connaître le score parfait
            long joursTotaux = java.time.temporal.ChronoUnit.DAYS.between(challenge.getDateDebut().toLocalDate(), challenge.getDateFin().toLocalDate()) + 1;

            for (var ligne : classement) {
                Utilisateur participant = ligne.getUtilisateur();

                // 3. Critère d'échec : par exemple, si l'utilisateur a validé moins de 50% du temps
                if (ligne.getScore() < (joursTotaux / 2.0)) {
                    // 🛑 Sanction : -20 HP pour l'échec
                    utilisateurService.appliquerPunitionChallenge(participant.getId(), 20);
                }
            }
        }
    }

    /**
     * 🔔 Chaque nuit, on sanctionne les participants qui n'ont rien indiqué la veille.
     * Le traitement est idempotent : une absence déjà enregistrée n'est pas rejouée.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void sanctionnerLesAbsencesQuotidiennes() {
        LocalDate hier = LocalDate.now().minusDays(1);
        challengeService.sanctionnerAbsencesQuotidiennes(hier);
    }
}

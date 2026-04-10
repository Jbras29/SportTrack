package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.NotificationItem;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Annonce;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.NotificationType;
import com.jocf.sporttrack.model.TypeCommentaire;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.AnnonceRepository;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.repository.ChallengeSaisieQuotidienneRepository;
import com.jocf.sporttrack.repository.CommentaireRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class NotificationService {

    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final CommentaireRepository commentaireRepository;
    private final AnnonceRepository annonceRepository;
    private final ActiviteRepository activiteRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Value("${sporttrack.notifications.jours-sans-activite-rappel:7}")
    private int joursSansActiviteRappel;

    public NotificationService(
            CommentaireRepository commentaireRepository,
            AnnonceRepository annonceRepository,
            ActiviteRepository activiteRepository,
            ChallengeRepository challengeRepository,
            ChallengeSaisieQuotidienneRepository challengeSaisieQuotidienneRepository,
            UtilisateurRepository utilisateurRepository) {
        this.commentaireRepository = commentaireRepository;
        this.annonceRepository = annonceRepository;
        this.activiteRepository = activiteRepository;
        this.challengeRepository = challengeRepository;
        this.challengeSaisieQuotidienneRepository = challengeSaisieQuotidienneRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Agrège toutes les sources de notifications pour l’utilisateur, du plus récent au plus ancien.
     */
    public List<NotificationItem> listerPourUtilisateur(Long utilisateurId) {
        Utilisateur proprietaire = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

        List<NotificationItem> items = new ArrayList<>();

        for (Commentaire c : commentaireRepository.findPourProprietaireActiviteEtType(utilisateurId, TypeCommentaire.REACTION)) {
            Utilisateur aut = c.getAuteur();
            Activite act = c.getActivite();
            String nomAuteur = aut.getPrenom() + " " + aut.getNom();
            items.add(new NotificationItem(
                    NotificationType.REACTION,
                    "Réaction à votre activité",
                    nomAuteur + " a réagi avec " + c.getMessage() + " à « " + act.getNom() + " »",
                    c.getDateCreation(),
                    "/home#post-" + act.getId(),
                    c.getId()
            ));
        }

        for (Commentaire c : commentaireRepository.findPourProprietaireActiviteEtType(utilisateurId, TypeCommentaire.MESSAGE)) {
            Utilisateur aut = c.getAuteur();
            Activite act = c.getActivite();
            String nomAuteur = aut.getPrenom() + " " + aut.getNom();
            String extrait = c.getMessage();
            if (extrait.length() > 160) {
                extrait = extrait.substring(0, 157) + "…";
            }
            items.add(new NotificationItem(
                    NotificationType.REPONSE_ACTIVITE,
                    "Commentaire sur votre activité",
                    nomAuteur + " sur « " + act.getNom() + " » : " + extrait,
                    c.getDateCreation(),
                    "/home#post-" + act.getId(),
                    c.getId()
            ));
        }

        for (Annonce a : annonceRepository.findAnnoncesPourEvenementsOuUtilisateurParticipe(utilisateurId)) {
            String titreEvenement = a.getEvenement().getNom();
            String msg = a.getMessage();
            if (msg.length() > 200) {
                msg = msg.substring(0, 197) + "…";
            }
            items.add(new NotificationItem(
                    NotificationType.ANNONCE_EVENEMENT,
                    "Annonce : " + titreEvenement,
                    msg,
                    a.getDate(),
                    "/evenements/" + a.getEvenement().getId(),
                    a.getId()
            ));
        }

        if (joursSansActiviteRappel > 0) {
            ajouterRappelInactiviteSiBesoin(items, proprietaire);
        }

        LocalDate aujourdhui = LocalDate.now();
        for (Challenge ch : challengeRepository.findByParticipants_IdOrderByDateFinAsc(utilisateurId)) {
            if (!challengeActifPourDate(ch, aujourdhui)) {
                continue;
            }
            if (!challengeSaisieQuotidienneRepository.existsByChallenge_IdAndUtilisateur_IdAndJour(
                    ch.getId(), utilisateurId, aujourdhui)) {
                items.add(new NotificationItem(
                        NotificationType.RAPPEL_CHALLENGE_QUOTIDIEN,
                        "Défi du jour",
                        "Indiquez si vous avez réalisé l’objectif du jour pour le challenge « " + ch.getNom() + " ».",
                        LocalDateTime.now(),
                        "/challenges/" + ch.getId(),
                        ch.getId()
                ));
            }
        }

        items.sort(Comparator.comparing(NotificationItem::dateTri).reversed());
        return items;
    }

    private void ajouterRappelInactiviteSiBesoin(List<NotificationItem> items, Utilisateur utilisateur) {
        Optional<Activite> derniere = activiteRepository.findTopByUtilisateurOrderByDateDesc(utilisateur);
        LocalDate aujourdhui = LocalDate.now();
        boolean inactif;
        if (derniere.isEmpty()) {
            inactif = true;
        } else {
            long jours = ChronoUnit.DAYS.between(derniere.get().getDate(), aujourdhui);
            inactif = jours >= joursSansActiviteRappel;
        }
        if (!inactif) {
            return;
        }
        String detail;
        if (derniere.isEmpty()) {
            detail = "Vous n'avez pas encore enregistré d'activité.";
        } else {
            detail = "Dernière activité le "
                    + derniere.get().getDate().format(DATE_FR)
                    + " — pensez à noter vos séances.";
        }
        items.add(new NotificationItem(
                NotificationType.RAPPEL_ACTIVITE,
                "Rappel d’activité",
                detail,
                LocalDateTime.now(),
                "/activites/create",
                null
        ));
    }

    private static boolean challengeActifPourDate(Challenge c, LocalDate jour) {
        if (c.getDateDebut() != null && jour.isBefore(c.getDateDebut().toLocalDate())) {
            return false;
        }
        if (c.getDateFin() != null && jour.isAfter(c.getDateFin().toLocalDate())) {
            return false;
        }
        return true;
    }
}

package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.ChallengeRepository;
import com.jocf.sporttrack.service.UtilisateurService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/dashboard")
public class DashboardController {

    private static final String CARD_DATES = "dates";
    private static final String CARD_PROGRESSION = "progression";
    private static final String CARD_PROGRESSION_TEXTE = "progressionTexte";
    private static final String CARD_STATUT_TEMPS = "statutTemps";
    private static final String CARD_PROGRESS_PENDING = "En attente";

    private final UtilisateurService utilisateurService;
    private final ActiviteRepository activiteRepository;
    private final ChallengeRepository challengeRepository;

    public DashboardController(
            UtilisateurService utilisateurService,
            ActiviteRepository activiteRepository,
            ChallengeRepository challengeRepository) {
        this.utilisateurService = utilisateurService;
        this.activiteRepository = activiteRepository;
        this.challengeRepository = challengeRepository;
    }

    @GetMapping
    public String dashboard(Model model) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return "redirect:/Utilisateur/";
        }

        try {
            Utilisateur user = utilisateurService.trouverParEmail(authentication.getName());
            LocalDate aujourdHui = LocalDate.now();
            model.addAttribute("user", user);
            List<Activite> activitesUtilisateur = activiteRepository.findByUtilisateurOrderByDateDesc(user);

            populateUserStats(model, user);
            populateActivityStats(model, activitesUtilisateur, aujourdHui);
            populateFavoriteSport(model, activitesUtilisateur);
            populateLastActivity(model, activitesUtilisateur, aujourdHui);
            populateChallengeCards(model, user, aujourdHui);
            model.addAttribute("dernieresActivites", activitesUtilisateur.stream().limit(5).toList());
            model.addAttribute("semaineActivite", buildWeeklyActivitySeries(activitesUtilisateur, aujourdHui));
            return "dashboard";
        } catch (Exception e) {
            return "redirect:/Utilisateur/";
        }
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated();
    }

    private void populateUserStats(Model model, Utilisateur user) {
        model.addAttribute("level", user.getNiveauExperience());
        model.addAttribute("xpBarPercent", user.getPourcentageBarreExperience());
        model.addAttribute("xpDansNiveau", user.getXpDepuisSeuilNiveauExperience());
        model.addAttribute("xpMaxNiveau", user.getXpSeuilProchainNiveauExperience());
        model.addAttribute("hpBarPercent", (double) user.getHpNormalise());
    }

    private void populateActivityStats(Model model, List<Activite> activitesUtilisateur, LocalDate aujourdHui) {
        double totalKm = activitesUtilisateur.stream()
                .mapToDouble(a -> a.getDistance() != null ? a.getDistance() : 0.0)
                .sum();
        int minutesEffectif = activitesUtilisateur.stream()
                .mapToInt(a -> a.getTemps() != null ? a.getTemps() : 0)
                .sum();
        long nbActivites = activitesUtilisateur.size();

        double moyenneDistanceKm = nbActivites > 0 ? totalKm / nbActivites : 0.0;
        int moyenneTempsMinutes = nbActivites > 0 ? (int) Math.round((double) minutesEffectif / nbActivites) : 0;
        double allureMoyenne = totalKm > 0 ? minutesEffectif / totalKm : 0.0;

        LocalDate premierJourMois = aujourdHui.withDayOfMonth(1);
        List<Activite> activitesMois = filterActivitesParDate(activitesUtilisateur, premierJourMois, aujourdHui);
        double moisDistanceKm = activitesMois.stream()
                .mapToDouble(a -> a.getDistance() != null ? a.getDistance() : 0.0)
                .sum();
        int moisMinutes = activitesMois.stream()
                .mapToInt(a -> a.getTemps() != null ? a.getTemps() : 0)
                .sum();
        long moisActivites = activitesMois.size();

        LocalDate lundi = aujourdHui.with(DayOfWeek.MONDAY);
        LocalDate dimanche = lundi.plusDays(6);
        List<Activite> activitesSemaine = filterActivitesParDate(activitesUtilisateur, lundi, dimanche);
        double distanceSemaineKm = activitesSemaine.stream()
                .mapToDouble(a -> a.getDistance() != null ? a.getDistance() : 0.0)
                .sum();
        long activitesSemaineCount = activitesSemaine.size();

        model.addAttribute("totalDistanceKm", totalKm);
        model.addAttribute("totalTempsMinutes", minutesEffectif);
        model.addAttribute("nombreActivites", nbActivites);
        model.addAttribute("kcalEstime", Math.round(minutesEffectif * 5.5));
        model.addAttribute("moyenneDistanceKm", moyenneDistanceKm);
        model.addAttribute("moyenneTempsMinutes", moyenneTempsMinutes);
        model.addAttribute("allureMoyenne", formatAllure(allureMoyenne));
        model.addAttribute("moisDistanceKm", moisDistanceKm);
        model.addAttribute("moisTempsMinutes", moisMinutes);
        model.addAttribute("moisActivites", moisActivites);
        model.addAttribute("activitesSemaineCount", activitesSemaineCount);
        model.addAttribute("distanceSemaineKm", distanceSemaineKm);
        model.addAttribute("totalDistanceTexte", formatDistance(totalKm));
        model.addAttribute("totalTempsTexte", formatDuration(minutesEffectif));
        model.addAttribute("moyenneDistanceTexte", formatDistance(moyenneDistanceKm));
        model.addAttribute("moyenneTempsTexte", formatDuration(moyenneTempsMinutes));
        model.addAttribute("moisDistanceTexte", formatDistance(moisDistanceKm));
        model.addAttribute("moisTempsTexte", formatDuration(moisMinutes));
        model.addAttribute("distanceSemaineTexte", formatDistance(distanceSemaineKm));
        model.addAttribute("kcalEstimeTexte", Math.round(minutesEffectif * 5.5) + " kcal");
    }

    private void populateFavoriteSport(Model model, List<Activite> activitesUtilisateur) {
        Optional<Map.Entry<com.jocf.sporttrack.enumeration.TypeSport, Long>> sportFavori =
                activitesUtilisateur.stream()
                        .filter(a -> a.getTypeSport() != null)
                        .collect(Collectors.groupingBy(
                                Activite::getTypeSport,
                                LinkedHashMap::new,
                                Collectors.counting()))
                        .entrySet()
                        .stream()
                        .max(Map.Entry.comparingByValue());
        sportFavori.ifPresentOrElse(entry -> {
            model.addAttribute("sportFavori", entry.getKey().name().replace('_', ' '));
            model.addAttribute("sportFavoriSeances", entry.getValue());
        }, () -> {
            model.addAttribute("sportFavori", "Aucun");
            model.addAttribute("sportFavoriSeances", 0L);
        });
    }

    private void populateLastActivity(Model model, List<Activite> activitesUtilisateur, LocalDate aujourdHui) {
        Activite derniereActivite = activitesUtilisateur.stream().findFirst().orElse(null);
        if (derniereActivite != null && derniereActivite.getDate() != null) {
            long joursDepuisDerniereSeance = ChronoUnit.DAYS.between(derniereActivite.getDate(), aujourdHui);
            model.addAttribute("derniereSeanceDate", derniereActivite.getDate());
            model.addAttribute("derniereSeanceJours", joursDepuisDerniereSeance);
            model.addAttribute("derniereSeanceRelativeTexte", formatRelativeDays(joursDepuisDerniereSeance));
            model.addAttribute("derniereSeanceTexte", formatDateFr(derniereActivite.getDate()));
            return;
        }

        model.addAttribute("derniereSeanceDate", null);
        model.addAttribute("derniereSeanceJours", null);
        model.addAttribute("derniereSeanceRelativeTexte", null);
        model.addAttribute("derniereSeanceTexte", "Aucune séance");
    }

    private void populateChallengeCards(Model model, Utilisateur user, LocalDate aujourdHui) {
        List<Challenge> defis = challengeRepository.findByParticipants_IdOrderByDateFinAsc(user.getId())
                .stream()
                .filter(c -> c.getDateFin() == null || !c.getDateFin().toLocalDate().isBefore(aujourdHui))
                .toList();
        model.addAttribute("defisEnCours", defis);
        model.addAttribute("defisEnCoursAffichage", defis.stream()
                .map(defi -> buildChallengeCard(defi, aujourdHui))
                .toList());
    }

    private List<Map<String, Object>> buildWeeklyActivitySeries(List<Activite> activitesUtilisateur, LocalDate aujourdHui) {
        List<Activite> activitesSemaine = filterActivitesParDate(activitesUtilisateur, aujourdHui.with(DayOfWeek.MONDAY), aujourdHui.with(DayOfWeek.MONDAY).plusDays(6));
        int[] minutesParJour = new int[7];
        for (Activite activite : activitesSemaine) {
            if (activite.getDate() != null && activite.getTemps() != null && activite.getTemps() > 0) {
                int index = activite.getDate().getDayOfWeek().getValue() - 1;
                minutesParJour[index] += activite.getTemps();
            }
        }
        String[] labelsJours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        List<Map<String, Object>> weekPoints = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Map<String, Object> point = new LinkedHashMap<>();
            point.put("label", labelsJours[i]);
            point.put("minutes", minutesParJour[i]);
            weekPoints.add(point);
        }
        return weekPoints;
    }

    private static List<Activite> filterActivitesParDate(List<Activite> activites, LocalDate debut, LocalDate fin) {
        return activites.stream()
                .filter(a -> a.getDate() != null
                        && !a.getDate().isBefore(debut)
                        && !a.getDate().isAfter(fin))
                .toList();
    }

    private Map<String, Object> buildChallengeCard(Challenge defi, LocalDate aujourdHui) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("nom", defi.getNom());

        LocalDate debut = defi.getDateDebut() != null ? defi.getDateDebut().toLocalDate() : null;
        LocalDate fin = defi.getDateFin() != null ? defi.getDateFin().toLocalDate() : null;
        if (debut != null && fin != null) {
            int progression = calculerProgression(defi, aujourdHui);
            card.put(CARD_DATES, formatDateCourte(debut) + " - " + formatDateCourte(fin));
            card.put(CARD_PROGRESSION, progression);
            card.put(CARD_PROGRESSION_TEXTE, progression + "% accompli");
            card.put(CARD_STATUT_TEMPS, calculerStatutTemps(debut, fin, aujourdHui));
        } else if (debut != null) {
            card.put(CARD_DATES, "Début : " + formatDateCourte(debut));
            card.put(CARD_PROGRESSION, 0);
            card.put(CARD_PROGRESSION_TEXTE, CARD_PROGRESS_PENDING);
            card.put(CARD_STATUT_TEMPS, "Date de fin non définie");
        } else if (fin != null) {
            card.put(CARD_DATES, "Fin : " + formatDateCourte(fin));
            card.put(CARD_PROGRESSION, 0);
            card.put(CARD_PROGRESSION_TEXTE, CARD_PROGRESS_PENDING);
            card.put(CARD_STATUT_TEMPS, calculerStatutTemps(null, fin, aujourdHui));
        } else {
            card.put(CARD_DATES, "Dates non définies");
            card.put(CARD_PROGRESSION, 0);
            card.put(CARD_PROGRESSION_TEXTE, CARD_PROGRESS_PENDING);
            card.put(CARD_STATUT_TEMPS, "Aucune date disponible");
        }

        card.put("participants", defi.getParticipants() != null ? defi.getParticipants().size() : 0);
        card.put("objectifJour", defi.getObjectifJour());
        return card;
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.FRANCE, "%.1f km", distance);
    }

    private static String formatDuration(int minutes) {
        if (minutes <= 0) {
            return "0min";
        }
        int heures = minutes / 60;
        int reste = minutes % 60;
        if (heures <= 0) {
            return reste + "min";
        }
        return heures + "h " + String.format(Locale.FRANCE, "%02d", reste) + "min";
    }

    private static String formatAllure(double minutesParKm) {
        if (minutesParKm <= 0 || Double.isNaN(minutesParKm) || Double.isInfinite(minutesParKm)) {
            return "—";
        }
        int minutes = (int) Math.floor(minutesParKm);
        int secondes = (int) Math.round((minutesParKm - minutes) * 60);
        if (secondes == 60) {
            minutes += 1;
            secondes = 0;
        }
        return minutes + "'" + String.format(Locale.FRANCE, "%02d", secondes) + "\"/km";
    }

    private static String formatDateFr(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)) : "";
    }

    private static String formatDateCourte(LocalDate date) {
        return date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "";
    }

    private static String formatRelativeDays(long jours) {
        if (jours <= 0) {
            return "Aujourd'hui";
        }
        return jours == 1 ? "1 jour" : jours + " jours";
    }

    private static int calculerProgression(Challenge defi, LocalDate aujourdHui) {
        if (defi.getDateDebut() == null || defi.getDateFin() == null) {
            return 0;
        }
        LocalDate debut = defi.getDateDebut().toLocalDate();
        LocalDate fin = defi.getDateFin().toLocalDate();
        if (aujourdHui.isBefore(debut)) {
            return 0;
        }
        long totalJours = ChronoUnit.DAYS.between(debut, fin) + 1;
        if (totalJours <= 0) {
            totalJours = 1;
        }
        long joursEcoules = ChronoUnit.DAYS.between(debut, aujourdHui) + 1;
        int progression = (int) Math.round(100.0 * joursEcoules / totalJours);
        return Math.clamp(progression, 0, 100);
    }

    private static String calculerStatutTemps(LocalDate debut, LocalDate fin, LocalDate aujourdHui) {
        if (fin == null) {
            return "Date de fin non définie";
        }
        if (debut != null && aujourdHui.isBefore(debut)) {
            long joursAvantDebut = ChronoUnit.DAYS.between(aujourdHui, debut);
            return joursAvantDebut <= 1 ? "Commence demain" : "Commence dans " + joursAvantDebut + " jours";
        }
        if (aujourdHui.isAfter(fin)) {
            return "Terminé";
        }
        long joursRestants = ChronoUnit.DAYS.between(aujourdHui, fin);
        if (joursRestants <= 0) {
            return "Se termine aujourd'hui";
        }
        return joursRestants == 1 ? "1 jour restant" : joursRestants + " jours restants";
    }
}

package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Règles de déblocage des badges après création d'une activité (logique volontairement hardcodée).
 * <p>
 * Badges non implémentés faute de données dans {@link Activite} :
 * <ul>
 *   <li>{@code EARLY_BIRD} — il n'y a pas d'heure (seulement {@link java.time.LocalDate})</li>
 *   <li>{@code NO_EXCUSES} — pas de météo / conditions difficiles</li>
 * </ul>
 * Heuristiques approximatives :
 * <ul>
 *   <li>{@code NATURE_LOVER} — mots-clés dans le champ {@code location} (parc, forêt, montagne, etc.)</li>
 *   <li>{@code GLOBE_TROTTER} — au moins 2 lieux textuels distincts (pas de ville structurée)</li>
 * </ul>
 */
@Service
public class ActiviteBadgeEvaluationService {

    private static final EnumSet<TypeSport> DISCIPLINES_NATATION = EnumSet.of(TypeSport.NATATION);
    private static final EnumSet<TypeSport> DISCIPLINES_VELO = EnumSet.of(
            TypeSport.CYCLISME, TypeSport.VELO_ROUTE, TypeSport.VTT);
    private static final EnumSet<TypeSport> DISCIPLINES_COURSE = EnumSet.of(
            TypeSport.COURSE, TypeSport.COURSE_A_PIED, TypeSport.MARATHON, TypeSport.TRAIL);

    private static final String[] MOTS_CLES_NATURE = {
            "parc", "forêt", "foret", "forest", "montagne", "trail", "bois", "nature",
            "lac", "rivière", "riviere", "sentier", "randonnée", "randonnee", "rando", "extérieur", "exterieur"
    };

    private final ActiviteRepository activiteRepository;
    private final BadgeService badgeService;

    public ActiviteBadgeEvaluationService(ActiviteRepository activiteRepository, BadgeService badgeService) {
        this.activiteRepository = activiteRepository;
        this.badgeService = badgeService;
    }

    /**
     * À appeler juste après la persistance de l'activité (incluse dans les agrégats).
     */
    @Transactional
    public void evaluerEtAttribuerBadges(Activite activite) {
        if (activite == null || activite.getUtilisateur() == null || activite.getUtilisateur().getId() == null) {
            return;
        }
        Long utilisateurId = activite.getUtilisateur().getId();
        Utilisateur utilisateur = activite.getUtilisateur();
        List<Activite> toutes = activiteRepository.findByUtilisateur(utilisateur);

        double kmCourant = Utilisateur.distanceEnKmPourFormuleXp(activite.getDistance());
        Integer tempsCourant = activite.getTemps();

        evaluerPremiersPasEtVolume(utilisateurId, toutes, kmCourant);
        evaluerTempsEtChrono(utilisateurId, toutes, tempsCourant);
        evaluerRecordPersonnel(utilisateurId, activite, toutes, kmCourant, tempsCourant);
        evaluerSeriesEtVolumeActivites(utilisateurId, activite, toutes);
        evaluerLieuxEtNature(utilisateurId, activite, toutes);
        evaluerMultiSports(utilisateurId, toutes);
    }

    private void evaluerPremiersPasEtVolume(Long utilisateurId, List<Activite> toutes, double kmCourant) {
        if (toutes.size() == 1) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "PREMIER_PAS");
        }
        if (kmCourant >= 5.0) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "CINQ_K_STARTER");
        }
        if (kmCourant >= 10.0) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "DIX_K_CONFIRME");
        }
        if (kmCourant >= 21.0) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "SEMI_HEROS");
        }
        if (kmCourant >= 42.0) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "MARATHONIEN");
        }
        double kmCumules = toutes.stream()
                .mapToDouble(a -> Utilisateur.distanceEnKmPourFormuleXp(a.getDistance()))
                .sum();
        if (kmCumules >= 100.0) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "CENT_KM_CUMULES");
        }
    }

    private void evaluerTempsEtChrono(Long utilisateurId, List<Activite> toutes, Integer tempsCourant) {
        long activitesAvecTemps = toutes.stream()
                .filter(a -> a.getTemps() != null && a.getTemps() > 0)
                .count();
        if (activitesAvecTemps == 1 && tempsCourant != null && tempsCourant > 0) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "PREMIER_CHRONO");
        }
        if (tempsCourant != null) {
            if (tempsCourant >= 60) {
                badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "ENDURANCE_1H");
            }
            if (tempsCourant >= 120) {
                badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "IRON_WILL_2H");
            }
        }
    }

    private void evaluerRecordPersonnel(
            Long utilisateurId,
            Activite activite,
            List<Activite> toutes,
            double kmCourant,
            Integer tempsCourant) {
        if (toutes.size() < 2) {
            return;
        }
        double maxKmAutres = toutes.stream()
                .filter(a -> activite.getId() == null || !activite.getId().equals(a.getId()))
                .mapToDouble(a -> Utilisateur.distanceEnKmPourFormuleXp(a.getDistance()))
                .max()
                .orElse(0.0);
        int maxTempsAutres = toutes.stream()
                .filter(a -> activite.getId() == null || !activite.getId().equals(a.getId()))
                .mapToInt(a -> a.getTemps() != null ? a.getTemps() : 0)
                .max()
                .orElse(0);

        boolean recordDistance = kmCourant > maxKmAutres && kmCourant > 0;
        boolean recordTemps = tempsCourant != null && tempsCourant > maxTempsAutres;

        if (recordDistance || recordTemps) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "RECORD_PERSONNEL");
        }
    }

    private void evaluerSeriesEtVolumeActivites(Long utilisateurId, Activite activite, List<Activite> toutes) {
        Set<java.time.LocalDate> dates = toutes.stream()
                .map(Activite::getDate)
                .collect(Collectors.toSet());
        java.time.LocalDate d = activite.getDate();
        if (d != null && suiteConsecutiveDepuis(d, dates, 7)) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "SERIE_7_JOURS");
        }
        if (d != null && suiteConsecutiveDepuis(d, dates, 30)) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "STREAK_30_JOURS");
        }
        if (toutes.size() >= 100) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "CENT_ACTIVITES");
        }
    }

    private void evaluerLieuxEtNature(Long utilisateurId, Activite activite, List<Activite> toutes) {
        long lieuxDistincts = toutes.stream()
                .map(Activite::getLocation)
                .filter(s -> s != null && !s.isBlank())
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .distinct()
                .count();
        if (lieuxDistincts >= 3) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "EXPLORATEUR_3_LIEUX");
        }
        if (lieuxDistincts >= 2) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "GLOBE_TROTTER");
        }

        String loc = activite.getLocation();
        if (loc != null && !loc.isBlank() && locationSembleNature(loc)) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "NATURE_LOVER");
        }
    }

    private void evaluerMultiSports(Long utilisateurId, List<Activite> toutes) {
        long typesDistincts = toutes.stream().map(Activite::getTypeSport).distinct().count();
        if (typesDistincts >= 3) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "POLYVALENT_3_SPORTS");
        }

        boolean aNatation = toutes.stream().anyMatch(a -> DISCIPLINES_NATATION.contains(a.getTypeSport()));
        boolean aVelo = toutes.stream().anyMatch(a -> DISCIPLINES_VELO.contains(a.getTypeSport()));
        boolean aCourse = toutes.stream().anyMatch(a -> DISCIPLINES_COURSE.contains(a.getTypeSport()));
        if (aNatation && aVelo && aCourse) {
            badgeService.attribuerBadgeParCodeSiPresent(utilisateurId, "TRIATHLETE");
        }
    }

    private static boolean suiteConsecutiveDepuis(java.time.LocalDate fin, Set<java.time.LocalDate> dates, int jours) {
        for (int i = 0; i < jours; i++) {
            if (!dates.contains(fin.minusDays(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean locationSembleNature(String location) {
        String lower = location.toLowerCase(Locale.ROOT);
        for (String mot : MOTS_CLES_NATURE) {
            if (lower.contains(mot)) {
                return true;
            }
        }
        return false;
    }
}

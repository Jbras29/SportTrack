package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.CreerActiviteCommand;
import com.jocf.sporttrack.dto.ModifierActiviteRequest;
import com.jocf.sporttrack.model.Activite;
import com.jocf.sporttrack.model.TypeSport;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ActiviteRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
@Service
public class ActiviteService {

    private static final String MSG_UTILISATEUR_INTROUVABLE = "Utilisateur introuvable : ";

    private final ActiviteRepository activiteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;
    private final ActiviteBadgeEvaluationService activiteBadgeEvaluationService;
    private final OpenMeteoService openMeteoService;

    public ActiviteService(
            ActiviteRepository activiteRepository,
            UtilisateurRepository utilisateurRepository,
            UtilisateurService utilisateurService,
            ActiviteBadgeEvaluationService activiteBadgeEvaluationService,
            OpenMeteoService openMeteoService) {
        this.activiteRepository = activiteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurService = utilisateurService;
        this.activiteBadgeEvaluationService = activiteBadgeEvaluationService;
        this.openMeteoService = openMeteoService;
    }

    public List<Activite> recupererToutesLesActivites() {
        return activiteRepository.findAll();
    }

    public Optional<Activite> trouverParId(Long id) {
        return activiteRepository.findById(id);
    }

    public List<Activite> recupererActivitesParUtilisateur(Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));
        return activiteRepository.findByUtilisateur(utilisateur);
    }

    public List<Activite> recupererActivitesPourProfil(Utilisateur utilisateur) {
        return activiteRepository.findByUtilisateurOrderByDateDesc(utilisateur);
    }

    public List<Activite> recupererActivitesParTypeSport(TypeSport typeSport) {
        return activiteRepository.findByTypeSport(typeSport);
    }

    public List<Activite> recupererActivitesDesAmis(Utilisateur utilisateur) {
        List<Utilisateur> amis = utilisateur.getAmis();
        if (amis == null || amis.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = amis.stream().map(Utilisateur::getId).toList();
        return activiteRepository.findByUtilisateurIdsWithUtilisateurOrderByDateDesc(ids);
    }

    public List<Activite> recupererActivitesFilActualite(Utilisateur utilisateur) {
        List<Long> ids = new ArrayList<>();
        ids.add(utilisateur.getId());
        List<Utilisateur> amis = utilisateur.getAmis();
        if (amis != null) {
            for (Utilisateur ami : amis) {
                ids.add(ami.getId());
            }
        }
        return activiteRepository.findByUtilisateurIdsWithUtilisateurOrderByDateDesc(ids);
    }

    public Activite creerActivite(CreerActiviteCommand cmd) {
        verifierDateActiviteNonFuture(cmd.date());

        Utilisateur utilisateur = utilisateurRepository.findById(cmd.utilisateurId())
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + cmd.utilisateurId()));

        double distanceBrute = cmd.distance() != null ? cmd.distance() : 0.0;
        int dureeMin = cmd.temps() != null ? cmd.temps() : 0;
        double distanceKm = Utilisateur.distanceEnKmPourFormuleXp(distanceBrute);
        int xpGagne = Utilisateur.calculerXpGagnePourActivite(distanceKm, dureeMin);

        List<Utilisateur> invites = cmd.invitesIds() != null
                ? utilisateurRepository.findAllById(cmd.invitesIds())
                : new ArrayList<>();

        Activite activite = Activite.builder()
                .nom(cmd.nom())
                .typeSport(cmd.typeSport())
                .date(cmd.date())
                .distance(distanceBrute)
                .temps(dureeMin)
                .location(cmd.location() != null ? cmd.location() : "")
                .evaluation(cmd.evaluation() != null ? cmd.evaluation() : 0)
                .xpGagne(xpGagne)
                .utilisateur(utilisateur)
                .invites(invites)
                .build();

        activite.setCalories(calculerKcalPourActivite(activite));

        OpenMeteoService.WeatherInfo meteo =
                openMeteoService.getWeatherForLocationAndDate(activite.getLocation(), activite.getDate());
        if (meteo != null) {
            activite.setMeteoTemperature(meteo.temperature());
            activite.setMeteoCondition(meteo.condition());
        }

        Activite sauvegardee = activiteRepository.save(activite);
        utilisateurService.crediterExperience(utilisateur, xpGagne);
        activiteBadgeEvaluationService.evaluerEtAttribuerBadges(sauvegardee);
        return sauvegardee;
    }

    public Activite creerActivite(Long utilisateurId, String nom, TypeSport typeSport, LocalDate date) {
        verifierDateActiviteNonFuture(date);

        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));

        int xpGagne = Utilisateur.calculerXpGagnePourActivite(0.0, 0);

        Activite activite = Activite.builder()
                .nom(nom)
                .typeSport(typeSport)
                .date(date)
                .xpGagne(xpGagne)
                .utilisateur(utilisateur)
                .build();

        Activite sauvegardee = activiteRepository.save(activite);
        utilisateurService.crediterExperience(utilisateur, xpGagne);
        activiteBadgeEvaluationService.evaluerEtAttribuerBadges(sauvegardee);
        return sauvegardee;
    }

    public Activite modifierActivite(Long id, ModifierActiviteRequest req) {
        Activite activite = activiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable : " + id));

        if (req.date() != null) {
            verifierDateActiviteNonFuture(req.date());
        }

        activite.setNom(req.nom());
        activite.setTypeSport(req.typeSport());
        activite.setDistance(req.distance());
        activite.setTemps(req.temps());
        activite.setDate(req.date());
        activite.setLocation(req.location());
        activite.setEvaluation(req.evaluation());
        activite.setInvites(new ArrayList<>(utilisateurRepository.findAllById(req.inviteIds())));

        activite.setCalories(calculerKcalPourActivite(activite));

        if (activite.getLocation() != null && !activite.getLocation().isEmpty()) {
            OpenMeteoService.WeatherInfo meteo =
                    openMeteoService.getWeatherForLocationAndDate(activite.getLocation(), activite.getDate());
            if (meteo != null) {
                activite.setMeteoTemperature(meteo.temperature());
                activite.setMeteoCondition(meteo.condition());
            }
        }

        return activiteRepository.save(activite);
    }

    public void supprimerActivite(Long id) {
        if (!activiteRepository.existsById(id)) {
            throw new IllegalArgumentException("Activite introuvable : " + id);
        }
        activiteRepository.deleteById(id);
    }

    private static void verifierDateActiviteNonFuture(LocalDate date) {
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de l'activité ne peut pas être dans le futur.");
        }
    }

    public Double calculerKilocalories(Long activiteId) {
        Activite activite = activiteRepository.findById(activiteId)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable : " + activiteId));
        return calculerKcalPourActivite(activite);
    }

    public Double calculerKcalPourActivite(Activite activite) {
        if (activite.getTemps() == null || activite.getTemps() == 0) {
            return 0.0;
        }

        double metValue = metPourTypeSport(activite.getTypeSport());
        double poidsUtilisateur = activite.getUtilisateur().getPoids() != null
                ? activite.getUtilisateur().getPoids()
                : 70.0;
        double dureeEnHeures = activite.getTemps() / 60.0;

        return Math.round(metValue * poidsUtilisateur * dureeEnHeures * 10.0) / 10.0;
    }

    private static double metPourTypeSport(TypeSport typeSport) {
        return switch (typeSport) {
            case COURSE, COURSE_A_PIED, MARATHON, TRAIL -> 10.0;
            case CYCLISME, VELO_ROUTE, VTT -> 8.0;
            case NATATION -> 7.0;
            case TRIATHLON, DUATHLON -> 9.0;
            case AVIRON, KAYAK, CANOE -> 7.0;
            case RANDONNEE, RAQUETTES -> 5.0;
            case FOOTBALL, BASKETBALL, HANDBALL, RUGBY, HOCKEY_GLACE, HOCKEY_GAZON -> 8.0;
            case VOLLEYBALL, BASEBALL, CRICKET, WATERPOLO -> 6.0;
            case TENNIS, SQUASH, BADMINTON -> 7.0;
            case PING_PONG, PADEL -> 5.0;
            case BOXE, MMA, MUAY_THAI, TAEKWONDO -> 10.0;
            case JUDO, KARATE, LUTTE, JUJITSU, AIKIDO, ESCRIME -> 7.0;
            case ALPINISME, ESCALADE -> 8.0;
            case SKI_ALPIN, SNOWBOARD -> 6.0;
            case SKI_DE_FOND -> 9.0;
            case SURF, KITESURF, WINDSURF -> 6.0;
            case PLONGEE -> 5.0;
            case MUSCULATION, CROSSFIT, POWERLIFTING, HALTEROPHILIE, CALISTHENICS -> 6.0;
            case YOGA, PILATES, STRETCHING -> 3.0;
            case SKATEBOARD, ROLLER, BMX -> 5.0;
            case PATINAGE_ARTISTIQUE, PATINAGE_VITESSE -> 6.0;
            case EQUITATION, POLO -> 5.0;
            case PARACHUTISME, PARAPENTE, VOL_LIBRE -> 4.0;
            case GOLF, BOWLING, TIRO_A_LARC, TIR, PETANQUE -> 3.0;
            case DANSE_SPORTIVE, ARTS_MARTIAUX -> 6.0;
            default -> 5.0;
        };
    }

    /**
     * Recalcule et persiste les calories + météo pour toutes les activités qui
     * n'ont pas encore ces informations (utile pour les données historiques).
     * Retourne le nombre d'activités mises à jour.
     */
    public int recalculerMeteoEtCaloriesPourToutesLesActivites() {
        List<Activite> toutes = activiteRepository.findAll();
        int count = 0;
        for (Activite activite : toutes) {
            boolean modifie = false;

            // Calories manquantes
            if (activite.getCalories() == null && activite.getTemps() != null && activite.getTemps() > 0) {
                activite.setCalories(calculerKcalPourActivite(activite));
                modifie = true;
            }

            // Météo manquante et lieu disponible
            if (activite.getMeteoCondition() == null
                    && activite.getLocation() != null
                    && !activite.getLocation().isBlank()) {
                OpenMeteoService.WeatherInfo meteo =
                        openMeteoService.getWeatherForLocationAndDate(activite.getLocation(), activite.getDate());
                if (meteo != null) {
                    activite.setMeteoTemperature(meteo.temperature());
                    activite.setMeteoCondition(meteo.condition());
                    modifie = true;
                }
            }

            if (modifie) {
                activiteRepository.save(activite);
                count++;
            }
        }
        return count;
    }
}
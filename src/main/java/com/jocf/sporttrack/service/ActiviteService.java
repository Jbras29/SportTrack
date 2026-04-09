package com.jocf.sporttrack.service;

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
import java.util.stream.Collectors;

@Service
public class ActiviteService {

    private final ActiviteRepository activiteRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final UtilisateurService utilisateurService;

    public ActiviteService(
            ActiviteRepository activiteRepository,
            UtilisateurRepository utilisateurRepository,
            UtilisateurService utilisateurService) {
        this.activiteRepository = activiteRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.utilisateurService = utilisateurService;
    }

    public List<Activite> recupererToutesLesActivites() {
        return activiteRepository.findAll();
    }

    public Optional<Activite> trouverParId(Long id) {
        return activiteRepository.findById(id);
    }

    public List<Activite> recupererActivitesParUtilisateur(Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));
        return activiteRepository.findByUtilisateur(utilisateur);
    }

    /**
     * Activités d’un profil, les plus récentes en premier (affichage identique au fil d’actualité).
     */
    public List<Activite> recupererActivitesPourProfil(Utilisateur utilisateur) {
        return activiteRepository.findByUtilisateurOrderByDateDesc(utilisateur);
    }

    public List<Activite> recupererActivitesParTypeSport(TypeSport typeSport) {
        return activiteRepository.findByTypeSport(typeSport);
    }

    /**
     * Activités des amis, les plus récentes en premier (par date d’activité).
     */
    public List<Activite> recupererActivitesDesAmis(Utilisateur utilisateur) {
        List<Utilisateur> amis = utilisateur.getAmis();
        if (amis == null || amis.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> ids = amis.stream().map(Utilisateur::getId).collect(Collectors.toList());
        return activiteRepository.findByUtilisateurIdsWithUtilisateurOrderByDateDesc(ids);
    }

    /**
     * Fil d’accueil : activités de l’utilisateur et de ses amis, les plus récentes en premier.
     */
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

    public Activite creerActivite(Long utilisateurId, String nom, TypeSport typeSport, LocalDate date, Double distance, Integer temps, String location, Integer evaluation) {
        verifierDateActiviteNonFuture(date);
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

        double distanceBrute = distance != null ? distance : 0.0;
        int dureeMin = temps != null ? temps : 0;
        double distanceKm = Utilisateur.distanceEnKmPourFormuleXp(distanceBrute);
        int xpGagne = Utilisateur.calculerXpGagnePourActivite(distanceKm, dureeMin);

        Activite activite = Activite.builder()
                .nom(nom)
                .typeSport(typeSport)
                .date(date)
                .distance(distanceBrute)
                .temps(dureeMin)
                .location(location != null ? location : "")
                .evaluation(evaluation != null ? evaluation : 0)
                .xpGagne(xpGagne)
                .utilisateur(utilisateur)
                .build();

        Activite sauvegardee = activiteRepository.save(activite);
        utilisateurService.crediterExperience(utilisateur, xpGagne);
        return sauvegardee;
    }

    public Activite creerActivite(Long utilisateurId, String nom, TypeSport typeSport, LocalDate date) {
        verifierDateActiviteNonFuture(date);
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + utilisateurId));

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
        return sauvegardee;
    }

    public Activite modifierActivite(Long id, Activite activiteDetails) {
        Activite activite = activiteRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Activite introuvable : " + id));

        if (activiteDetails.getDate() != null) {
            verifierDateActiviteNonFuture(activiteDetails.getDate());
        }

        activite.setNom(activiteDetails.getNom());
        activite.setTypeSport(activiteDetails.getTypeSport());
        activite.setDistance(activiteDetails.getDistance());
        activite.setTemps(activiteDetails.getTemps());
        activite.setDate(activiteDetails.getDate());
        activite.setLocation(activiteDetails.getLocation());
        activite.setEvaluation(activiteDetails.getEvaluation());

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
    
            double metValue = switch (activite.getTypeSport()) {
                // Sports d'endurance
                case COURSE, COURSE_A_PIED, MARATHON, TRAIL -> 10.0;
                case CYCLISME, VELO_ROUTE, VTT -> 8.0;
                case NATATION -> 7.0;
                case TRIATHLON, DUATHLON -> 9.0;
                case AVIRON, KAYAK, CANOE -> 7.0;
                case RANDONNEE, RAQUETTES -> 5.0;
            
                // Sports collectifs
                case FOOTBALL, BASKETBALL, HANDBALL, RUGBY, HOCKEY_GLACE, HOCKEY_GAZON -> 8.0;
                case VOLLEYBALL, BASEBALL, CRICKET, WATERPOLO -> 6.0;
            
                // Sports de raquette
                case TENNIS, SQUASH, BADMINTON -> 7.0;
                case PING_PONG, PADEL -> 5.0;
            
                // Sports de combat
                case BOXE, MMA, MUAY_THAI, TAEKWONDO -> 10.0;
                case JUDO, KARATE, LUTTE, JUJITSU, AIKIDO, ESCRIME -> 7.0;
            
                // Sports de montagne
                case ALPINISME, ESCALADE -> 8.0;
                case SKI_ALPIN, SNOWBOARD -> 6.0;
                case SKI_DE_FOND -> 9.0;
                case SURF, KITESURF, WINDSURF -> 6.0;
                case PLONGEE -> 5.0;
            
                // Fitness / force
                case MUSCULATION, CROSSFIT, POWERLIFTING, HALTÉROPHILIE, CALISTHENICS -> 6.0;
                case YOGA, PILATES, STRETCHING -> 3.0;
            
                // Glisse / vitesse
                case SKATEBOARD, ROLLER, BMX -> 5.0;
                case PATINAGE_ARTISTIQUE, PATINAGE_VITESSE -> 6.0;
            
                // Équestres
                case EQUITATION, POLO -> 5.0;
            
                // Aériens
                case PARACHUTISME, PARAPENTE, VOL_LIBRE -> 4.0;
            
                // Autres
                case GOLF, BOWLING, TIRO_A_LARC, TIR, PÉTANQUE -> 3.0;
                case DANSE_SPORTIVE, ARTS_MARTIAUX -> 6.0;
            
                default -> 5.0;
            };
    
        double poidsUtilisateur = activite.getUtilisateur().getPoids() != null
            ? activite.getUtilisateur().getPoids()
            : 70.0; // poids par défaut si non renseigné
    
        double dureeEnHeures = activite.getTemps() / 60.0;
    
        return metValue * poidsUtilisateur * dureeEnHeures;
    }
}
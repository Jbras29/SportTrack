package com.jocf.sporttrack.config;

import com.jocf.sporttrack.model.Badge;
import com.jocf.sporttrack.repository.BadgeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Insère les définitions de badges au démarrage si elles ne sont pas encore présentes
 * (idempotent par {@code code}) ; met à jour nom, photo et description si le code existe déjà.
 */
@Component
@Order(1)
public class BadgeDataInitializer implements CommandLineRunner {

    private final BadgeRepository badgeRepository;

    public BadgeDataInitializer(BadgeRepository badgeRepository) {
        this.badgeRepository = badgeRepository;
    }

    @Override
    public void run(String... args) {
        List<BadgeDef> definitions = List.of(
                new BadgeDef("PREMIER_PAS", "Premier pas",
                        "https://placehold.co/120x120/059669/ffffff/png?text=1er",
                        "Première activité enregistrée."),
                new BadgeDef("CINQ_K_STARTER", "5K Starter",
                        "https://placehold.co/120x120/2563eb/ffffff/png?text=5K",
                        "Premier parcours de 5 km."),
                new BadgeDef("DIX_K_CONFIRME", "10K Confirmé",
                        "https://placehold.co/120x120/7c3aed/ffffff/png?text=10K",
                        "Premier parcours de 10 km."),
                new BadgeDef("SEMI_HEROS", "Semi-héros",
                        "https://placehold.co/120x120/d97706/ffffff/png?text=21K",
                        "Premier semi-marathon (21 km)."),
                new BadgeDef("MARATHONIEN", "Marathonien",
                        "https://placehold.co/120x120/b91c1c/ffffff/png?text=42K",
                        "Premier marathon (42 km)."),
                new BadgeDef("CENT_KM_CUMULES", "100 km cumulés",
                        "https://placehold.co/120x120/0e7490/ffffff/png?text=100km",
                        "100 km cumulés, toutes activités confondues."),
                new BadgeDef("PREMIER_CHRONO", "Premier chrono",
                        "https://placehold.co/120x120/4f46e5/ffffff/png?text=Chrono",
                        "Première activité avec temps enregistré."),
                new BadgeDef("ENDURANCE_1H", "Endurance",
                        "https://placehold.co/120x120/047857/ffffff/png?text=1h",
                        "Activité de plus d'une heure."),
                new BadgeDef("IRON_WILL_2H", "Iron Will",
                        "https://placehold.co/120x120/9d174d/ffffff/png?text=2h",
                        "Activité de plus de deux heures."),
                new BadgeDef("RECORD_PERSONNEL", "Record personnel",
                        "https://placehold.co/120x120/ca8a04/1c1917/png?text=RP",
                        "Battre son meilleur temps ou sa meilleure distance."),
                new BadgeDef("SERIE_7_JOURS", "Série 7 jours",
                        "https://placehold.co/120x120/ea580c/ffffff/png?text=7j",
                        "Au moins une activité chaque jour pendant une semaine."),
                new BadgeDef("STREAK_30_JOURS", "Streak 30 jours",
                        "https://placehold.co/120x120/c2410c/ffffff/png?text=30j",
                        "Régularité sur un mois (série d'activités)."),
                new BadgeDef("CENT_ACTIVITES", "100 activités",
                        "https://placehold.co/120x120/4338ca/ffffff/png?text=100",
                        "Cap symbolique : 100 activités enregistrées."),
                new BadgeDef("EXPLORATEUR_3_LIEUX", "Explorateur",
                        "https://placehold.co/120x120/0d9488/ffffff/png?text=3lieux",
                        "Activités dans 3 lieux différents."),
                new BadgeDef("NATURE_LOVER", "Nature Lover",
                        "https://placehold.co/120x120/166534/ffffff/png?text=Nature",
                        "Activité en extérieur (parc ou forêt)."),
                new BadgeDef("GLOBE_TROTTER", "Globe-trotter",
                        "https://placehold.co/120x120/0369a1/ffffff/png?text=Globe",
                        "Activités dans plusieurs villes."),
                new BadgeDef("POLYVALENT_3_SPORTS", "Polyvalent",
                        "https://placehold.co/120x120/a21caf/ffffff/png?text=3sport",
                        "Pratiquer 3 sports différents."),
                new BadgeDef("TRIATHLETE", "Triathlète",
                        "https://placehold.co/120x120/be123c/ffffff/png?text=Tri",
                        "Les trois disciplines majeures (natation, vélo, course)."),
                new BadgeDef("EARLY_BIRD", "Early Bird",
                        "https://placehold.co/120x120/f59e0b/1c1917/png?text=Matin",
                        "Activité tôt le matin."),
                new BadgeDef("NO_EXCUSES", "No Excuses",
                        "https://placehold.co/120x120/57534e/ffffff/png?text=Pluie",
                        "Activité malgré des conditions difficiles (météo, etc.)."));

        for (BadgeDef d : definitions) {
            badgeRepository.findByCode(d.code()).ifPresentOrElse(
                    existing -> {
                        if (!Objects.equals(existing.getNom(), d.nom())
                                || !Objects.equals(existing.getPhoto(), d.photo())
                                || !Objects.equals(existing.getDescription(), d.description())) {
                            existing.setNom(d.nom());
                            existing.setPhoto(d.photo());
                            existing.setDescription(d.description());
                            badgeRepository.save(existing);
                        }
                    },
                    () -> badgeRepository.save(
                            new Badge(d.code(), d.nom(), d.photo(), d.description())));
        }
    }

    private record BadgeDef(String code, String nom, String photo, String description) {
    }
}

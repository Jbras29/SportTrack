package com.jocf.sporttrack.config;

import com.jocf.sporttrack.model.Badge;
import com.jocf.sporttrack.repository.BadgeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Insère les définitions de badges au démarrage si elles ne sont pas encore présentes
 * (idempotent par {@code code}).
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
                        "https://placehold.co/120x120/059669/ffffff/png?text=1er"),
                new BadgeDef("CINQ_K_STARTER", "5K Starter",
                        "https://placehold.co/120x120/2563eb/ffffff/png?text=5K"),
                new BadgeDef("DIX_K_CONFIRME", "10K Confirmé",
                        "https://placehold.co/120x120/7c3aed/ffffff/png?text=10K"),
                new BadgeDef("SEMI_HEROS", "Semi-héros",
                        "https://placehold.co/120x120/d97706/ffffff/png?text=21K"),
                new BadgeDef("MARATHONIEN", "Marathonien",
                        "https://placehold.co/120x120/b91c1c/ffffff/png?text=42K"),
                new BadgeDef("CENT_KM_CUMULES", "100 km cumulés",
                        "https://placehold.co/120x120/0e7490/ffffff/png?text=100km"),
                new BadgeDef("PREMIER_CHRONO", "Premier chrono",
                        "https://placehold.co/120x120/4f46e5/ffffff/png?text=Chrono"),
                new BadgeDef("ENDURANCE_1H", "Endurance",
                        "https://placehold.co/120x120/047857/ffffff/png?text=1h"),
                new BadgeDef("IRON_WILL_2H", "Iron Will",
                        "https://placehold.co/120x120/9d174d/ffffff/png?text=2h"),
                new BadgeDef("RECORD_PERSONNEL", "Record personnel",
                        "https://placehold.co/120x120/ca8a04/1c1917/png?text=RP"),
                new BadgeDef("SERIE_7_JOURS", "Série 7 jours",
                        "https://placehold.co/120x120/ea580c/ffffff/png?text=7j"),
                new BadgeDef("STREAK_30_JOURS", "Streak 30 jours",
                        "https://placehold.co/120x120/c2410c/ffffff/png?text=30j"),
                new BadgeDef("CENT_ACTIVITES", "100 activités",
                        "https://placehold.co/120x120/4338ca/ffffff/png?text=100"),
                new BadgeDef("EXPLORATEUR_3_LIEUX", "Explorateur",
                        "https://placehold.co/120x120/0d9488/ffffff/png?text=3lieux"),
                new BadgeDef("NATURE_LOVER", "Nature Lover",
                        "https://placehold.co/120x120/166534/ffffff/png?text=Nature"),
                new BadgeDef("GLOBE_TROTTER", "Globe-trotter",
                        "https://placehold.co/120x120/0369a1/ffffff/png?text=Globe"),
                new BadgeDef("POLYVALENT_3_SPORTS", "Polyvalent",
                        "https://placehold.co/120x120/a21caf/ffffff/png?text=3sport"),
                new BadgeDef("TRIATHLETE", "Triathlète",
                        "https://placehold.co/120x120/be123c/ffffff/png?text=Tri"),
                new BadgeDef("EARLY_BIRD", "Early Bird",
                        "https://placehold.co/120x120/f59e0b/1c1917/png?text=Matin"),
                new BadgeDef("NO_EXCUSES", "No Excuses",
                        "https://placehold.co/120x120/57534e/ffffff/png?text=Pluie"));

        for (BadgeDef d : definitions) {
            if (!badgeRepository.existsByCode(d.code())) {
                badgeRepository.save(new Badge(d.code(), d.nom(), d.photo()));
            }
        }
    }

    private record BadgeDef(String code, String nom, String photo) {
    }
}

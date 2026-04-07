package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Badge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.BadgeRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UtilisateurRepository utilisateurRepository;

    // Injection des dépendances via le constructeur
    @Autowired
    public BadgeService(BadgeRepository badgeRepository, UtilisateurRepository utilisateurRepository) {
        this.badgeRepository = badgeRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    // Vérifie et attribue les badges à un utilisateur en fonction des kilomètres qu'il vient de parcourir
    @Transactional
    public void attribuerBadgesParDistance(Long utilisateurId, double distanceParcourue) {
        // 1. Récupérer l'utilisateur depuis la base de données
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + utilisateurId));

        // 2. Convertir la distance en entier (car le champ xpNecessaire de votre entité est un int)
        int distanceArrondie = (int) distanceParcourue;

        // 3. Trouver tous les badges que cette distance permet de débloquer dans la base de données
        List<Badge> badgesDebloquables = badgeRepository.findByKmNecessaireLessThanEqual(distanceArrondie);

        // 4. Vérifier si l'utilisateur possède déjà ces badges, sinon les lui ajouter
        boolean utilisateurMisAJour = false;
        List<Badge> badgesDeLUtilisateur = utilisateur.getBadges();

        for (Badge badge : badgesDebloquables) {
            // Vérifie si l'utilisateur ne possède pas encore ce badge
            if (!badgesDeLUtilisateur.contains(badge)) {
                badgesDeLUtilisateur.add(badge);
                utilisateurMisAJour = true;
            }
        }

        // 5. Sauvegarder l'utilisateur uniquement si de nouveaux badges ont été ajoutés
        if (utilisateurMisAJour) {
            utilisateurRepository.save(utilisateur);
        }
    }

    // Récupère la liste de tous les badges possédés par un utilisateur spécifique
    public List<Badge> obtenirBadgesUtilisateur(Long utilisateurId) {
        // Récupère l'utilisateur et retourne sa liste de badges
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return utilisateur.getBadges();
    }
}
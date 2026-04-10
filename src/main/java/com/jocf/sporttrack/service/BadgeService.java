package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Badge;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.BadgeRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BadgeService {

    private final BadgeRepository badgeRepository;
    private final UtilisateurRepository utilisateurRepository;

    public BadgeService(BadgeRepository badgeRepository, UtilisateurRepository utilisateurRepository) {
        this.badgeRepository = badgeRepository;
        this.utilisateurRepository = utilisateurRepository;
    }

    /**
     * Attribue un badge à l’utilisateur s’il existe en base et n’est pas déjà possédé.
     * À appeler depuis les règles métier (distance, streak, XP, etc.).
     */
    @Transactional
    public void attribuerBadgeParCode(Long utilisateurId, String badgeCode) {
        Badge badge = badgeRepository.findByCode(badgeCode)
                .orElseThrow(() -> new IllegalArgumentException("Badge inconnu: " + badgeCode));
        attribuerBadgeSiAbsent(utilisateurId, badge);
    }

    /**
     * Comme {@link #attribuerBadgeParCode} mais ignore silencieusement un code inconnu (seed / évolution du catalogue).
     */
    @Transactional
    public void attribuerBadgeParCodeSiPresent(Long utilisateurId, String badgeCode) {
        badgeRepository.findByCode(badgeCode)
                .ifPresent(badge -> attribuerBadgeSiAbsent(utilisateurId, badge));
    }

    private void attribuerBadgeSiAbsent(Long utilisateurId, Badge badge) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec l'ID: " + utilisateurId));

        List<Badge> badges = utilisateur.getBadges();
        if (!badges.contains(badge)) {
            badges.add(badge);
            utilisateurRepository.save(utilisateur);
        }
    }

    public List<Badge> obtenirBadgesUtilisateur(Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        return utilisateur.getBadges();
    }

    public List<Badge> listerTousLesBadges() {
        return badgeRepository.findAll();
    }
}

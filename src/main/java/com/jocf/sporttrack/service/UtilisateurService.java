package com.jocf.sporttrack.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jocf.sporttrack.dto.ModifierUtilisateurRequest;
import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.PrefSportiveRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;

@Service
public class UtilisateurService implements UserDetailsService {

    private static final String MSG_UTILISATEUR_INTROUVABLE = "Utilisateur introuvable : ";

    private final UtilisateurRepository utilisateurRepository;
    private final PrefSportiveRepository prefSportiveRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<AuthenticationConfiguration> authenticationConfigurationProvider;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            PrefSportiveRepository prefSportiveRepository,
            PasswordEncoder passwordEncoder,
            ObjectProvider<AuthenticationConfiguration> authenticationConfigurationProvider) {
        this.utilisateurRepository = utilisateurRepository;
        this.prefSportiveRepository = prefSportiveRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationConfigurationProvider = authenticationConfigurationProvider;
    }

    public List<Utilisateur> recupererTousLesUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    public Optional<Utilisateur> trouverParId(Long id) {
        return utilisateurRepository.findById(id);
    }

    public Utilisateur modifierUtilisateur(Long id, ModifierUtilisateurRequest req) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + id));

        utilisateur.setNom(req.getNom());
        utilisateur.setPrenom(req.getPrenom());
        utilisateur.setEmail(req.getEmail());
        String nouveauMotdepasse = req.getMotdepasse();
        if (nouveauMotdepasse != null && nouveauMotdepasse.isBlank()) {
            nouveauMotdepasse = null;
        }
        utilisateur.setMotdepasse(nouveauMotdepasse != null
                ? passwordEncoder.encode(nouveauMotdepasse)
                : utilisateur.getMotdepasse());
        utilisateur.setSexe(req.getSexe());
        utilisateur.setAge(req.getAge());
        utilisateur.setPoids(req.getPoids());
        utilisateur.setTaille(req.getTaille());
        utilisateur.setObjectifsPersonnels(req.getObjectifsPersonnels());
        utilisateur.setNiveauPratiqueSportive(req.getNiveauPratiqueSportive());
        utilisateur.setComptePrive(req.isComptePrive());

        if (req.getPhotoProfil() != null) {
            utilisateur.setPhotoProfil(req.getPhotoProfil().isBlank()
                    ? null
                    : req.getPhotoProfil());
        }

        if (req.getPrefSportivesIds() != null) {
            utilisateur.getPrefSportives().clear();
            for (Long prefId : req.getPrefSportivesIds()) {
                if (prefId != null) {
                    prefSportiveRepository.findById(prefId)
                            .ifPresent(utilisateur.getPrefSportives()::add);
                }
            }
        }

        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur ajouterPrefSportive(Long utilisateurId, String nomPreference) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));

        String nomNormalise = normaliserNomPreference(nomPreference);
        boolean dejaAssociee = utilisateur.getPrefSportives().stream()
                .anyMatch(pref -> pref.getNom() != null && pref.getNom().equalsIgnoreCase(nomNormalise));
        if (dejaAssociee) {
            throw new IllegalArgumentException("Cette preference sportive existe deja dans votre profil.");
        }

        PrefSportive preference = prefSportiveRepository.findByNomIgnoreCase(nomNormalise)
                .orElseGet(() -> prefSportiveRepository.save(PrefSportive.builder().nom(nomNormalise).build()));

        utilisateur.getPrefSportives().add(preference);
        trierPreferences(utilisateur);
        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur supprimerPrefSportive(Long utilisateurId, Long prefSportiveId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));

        boolean supprimee = utilisateur.getPrefSportives().removeIf(pref -> prefSportiveId.equals(pref.getId()));
        if (!supprimee) {
            throw new IllegalArgumentException("Preference sportive introuvable sur ce profil.");
        }

        return utilisateurRepository.save(utilisateur);
    }

    public void modifierPhotoProfil(Long id, String cheminPublic) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + id));
        utilisateur.setPhotoProfil(cheminPublic);
        utilisateurRepository.save(utilisateur);
    }

    @Transactional
    public void enregistrerDerniereConsultationNotifications(Long utilisateurId) {
        Utilisateur utilisateur = utilisateurRepository.findById(utilisateurId)
                .orElseThrow(() -> new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + utilisateurId));
        utilisateur.setDerniereConsultationNotifications(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);
    }

    /**
     * Ajoute de l’expérience au compte (ex. après enregistrement d’une activité). Persiste immédiatement.
     */
    public Utilisateur crediterExperience(Utilisateur utilisateur, int montantXp) {
        if (montantXp <= 0) {
            return utilisateur;
        }
        utilisateur.setXp(utilisateur.getXpEffectif() + montantXp);
        return utilisateurRepository.save(utilisateur);
    }

    public void supprimerUtilisateur(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new IllegalArgumentException(MSG_UTILISATEUR_INTROUVABLE + id);
        }
        utilisateurRepository.deleteById(id);
    }

    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new IllegalArgumentException("Ce username est deja utilise.");
        }

        utilisateur.setId(null);
        utilisateur.setMotdepasse(passwordEncoder.encode(utilisateur.getMotdepasse()));
        utilisateur.setXp(utilisateur.getXp() != null ? utilisateur.getXp() : 0);
        utilisateur.setHp(utilisateur.getHp() != null ? utilisateur.getHp() : 100);

        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur trouverParEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(MSG_UTILISATEUR_INTROUVABLE + email));
    }

    public Utilisateur trouverParEmailAvecAmis(String email) {
        return utilisateurRepository.findByEmailWithAmis(email)
                .orElseThrow(() -> new UsernameNotFoundException(MSG_UTILISATEUR_INTROUVABLE + email));
    }

    public Utilisateur connecter(String email, String motdepasse) {
        Authentication authentication = getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(email, motdepasse));

        return trouverParEmail(authentication.getName());
    }

    private org.springframework.security.authentication.AuthenticationManager getAuthenticationManager() {
        try {
            AuthenticationConfiguration authenticationConfiguration = authenticationConfigurationProvider.getIfAvailable();
            if (authenticationConfiguration == null) {
                throw new IllegalStateException("Impossible d'initialiser AuthenticationManager.");
            }
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception exception) {
            throw new IllegalStateException("Impossible d'initialiser AuthenticationManager.", exception);
        }
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utilisateur utilisateur = trouverParEmail(email);

        return User.withUsername(utilisateur.getEmail())
                .password(utilisateur.getMotdepasse())
                .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
                .build();
    }

    public List<Utilisateur> rechercherParNom(String prenom) {
        return utilisateurRepository.findByPrenomContainingIgnoreCase(prenom);
    }

    private String normaliserNomPreference(String nomPreference) {
        if (nomPreference == null) {
            throw new IllegalArgumentException("Le nom de la preference sportive est obligatoire.");
        }

        String nomNormalise = nomPreference.trim().replaceAll("\\s+", " ");
        if (nomNormalise.isBlank()) {
            throw new IllegalArgumentException("Le nom de la preference sportive est obligatoire.");
        }

        return nomNormalise;
    }

    private void trierPreferences(Utilisateur utilisateur) {
        utilisateur.getPrefSportives().sort(Comparator.comparing(
                PrefSportive::getNom,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
    }

    public Utilisateur findById(Long id) {
        return utilisateurRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec id : " + id));
    }

    public Utilisateur findByIdWithAmis(Long id) {
        return utilisateurRepository.findByIdWithAmis(id)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé avec id : " + id));
    }

    public List<Utilisateur> listerAmisTries(Utilisateur utilisateur) {
        List<Utilisateur> amis = new ArrayList<>(utilisateur.getAmis());
        amis.sort(Comparator.comparing(Utilisateur::getPrenom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(Utilisateur::getNom, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        return amis;
    }

    /**
     * Indique si le visiteur peut voir le nom et l’identité complète du sujet (profil public, soi-même, ou ami si compte privé).
     */
    public boolean peutAfficherIdentiteVers(Utilisateur sujet, Long visiteurId) {
        if (sujet == null || visiteurId == null) {
            return false;
        }
        if (sujet.getId().equals(visiteurId)) {
            return true;
        }
        if (!sujet.isComptePrive()) {
            return true;
        }
        return utilisateurRepository.findAmiIdsByUtilisateurId(visiteurId).contains(sujet.getId());
    }

    //Supprimer un ami de la liste d'amis de l'utilisateur
    public void supprimerAmi(Long utilisateurId, Long amiId) {
            Utilisateur utilisateur = utilisateurRepository.findByIdWithAmis(utilisateurId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé : " + utilisateurId));
            Utilisateur ami = utilisateurRepository.findByIdWithAmis(amiId)
                    .orElseThrow(() -> new RuntimeException("Ami non trouvé : " + amiId));

            utilisateur.getAmis().remove(ami);
            ami.getAmis().remove(utilisateur); 

            utilisateurRepository.save(utilisateur);
            utilisateurRepository.save(ami);
        }


    @Transactional
    public void appliquerPunitionChallenge(Long userId, int pointsARetirer) {
        utilisateurRepository.findById(userId).ifPresent(user -> {
            // Appliquer la soustraction (via ta méthode dans Utilisateur.java)
            user.soustraireHp(pointsARetirer);
            utilisateurRepository.save(user);
        });
    }
    }

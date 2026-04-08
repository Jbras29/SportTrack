package com.jocf.sporttrack.service;

import java.util.List;
import java.util.Optional;
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

import com.jocf.sporttrack.model.PrefSportive;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.PrefSportiveRepository;
import com.jocf.sporttrack.repository.UtilisateurRepository;

@Service
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final PrefSportiveRepository prefSportiveRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationConfiguration authenticationConfiguration;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            PrefSportiveRepository prefSportiveRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationConfiguration authenticationConfiguration) {
        this.utilisateurRepository = utilisateurRepository;
        this.prefSportiveRepository = prefSportiveRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationConfiguration = authenticationConfiguration;
    }

    public List<Utilisateur> recupererTousLesUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    public Optional<Utilisateur> trouverParId(Long id) {
        return utilisateurRepository.findById(id);
    }

    public Utilisateur modifierUtilisateur(Long id, Utilisateur utilisateurDetails) {
        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable : " + id));

        utilisateur.setNom(utilisateurDetails.getNom());
        utilisateur.setPrenom(utilisateurDetails.getPrenom());
        utilisateur.setEmail(utilisateurDetails.getEmail());
        String nouveauMotdepasse = utilisateurDetails.getMotdepasse();
        if (nouveauMotdepasse != null && nouveauMotdepasse.isBlank()) {
            nouveauMotdepasse = null;
        }
        utilisateur.setMotdepasse(nouveauMotdepasse != null
                ? passwordEncoder.encode(nouveauMotdepasse)
                : utilisateur.getMotdepasse());
        utilisateur.setSexe(utilisateurDetails.getSexe());
        utilisateur.setAge(utilisateurDetails.getAge());
        utilisateur.setPoids(utilisateurDetails.getPoids());
        utilisateur.setTaille(utilisateurDetails.getTaille());
        if (utilisateurDetails.getXp() != null) {
            utilisateur.setXp(utilisateurDetails.getXp());
        }
        if (utilisateurDetails.getHp() != null) {
            utilisateur.setHp(utilisateurDetails.getHp());
        }
        utilisateur.setObjectifsPersonnels(utilisateurDetails.getObjectifsPersonnels());
        utilisateur.setNiveauPratiqueSportive(utilisateurDetails.getNiveauPratiqueSportive());

        if (utilisateurDetails.getPrefSportives() != null) {
            utilisateur.getPrefSportives().clear();
            for (PrefSportive preference : utilisateurDetails.getPrefSportives()) {
                if (preference.getId() != null) {
                    prefSportiveRepository.findById(preference.getId())
                            .ifPresent(utilisateur.getPrefSportives()::add);
                }
            }
        }

        return utilisateurRepository.save(utilisateur);
    }

    public void supprimerUtilisateur(Long id) {
        if (!utilisateurRepository.existsById(id)) {
            throw new IllegalArgumentException("Utilisateur introuvable : " + id);
        }
        utilisateurRepository.deleteById(id);
    }

    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByEmail(utilisateur.getEmail())) {
            throw new IllegalArgumentException("Ce email est deja utilise.");
        }

        utilisateur.setId(null);
        utilisateur.setMotdepasse(passwordEncoder.encode(utilisateur.getMotdepasse()));
        utilisateur.setXp(utilisateur.getXp() != null ? utilisateur.getXp() : 0);
        utilisateur.setHp(utilisateur.getHp() != null ? utilisateur.getHp() : 100);

        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur trouverParEmail(String email) {
        return utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + email));
    }

    public Utilisateur connecter(String email, String motdepasse) {
        Authentication authentication = getAuthenticationManager().authenticate(
                new UsernamePasswordAuthenticationToken(email, motdepasse));

        return trouverParEmail(authentication.getName());
    }

    private org.springframework.security.authentication.AuthenticationManager getAuthenticationManager() {
        try {
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
}

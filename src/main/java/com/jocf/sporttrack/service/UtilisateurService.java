package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.UtilisateurRepository;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UtilisateurService implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public UtilisateurService(
            UtilisateurRepository utilisateurRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    public List<Utilisateur> recupererTousLesUtilisateurs() {
        return utilisateurRepository.findAll();
    }

    public Utilisateur creerUtilisateur(Utilisateur utilisateur) {
        if (utilisateurRepository.existsByUsername(utilisateur.getUsername())) {
            throw new IllegalArgumentException("Ce username est deja utilise.");
        }

        utilisateur.setId(null);
        utilisateur.setMotdepasse(passwordEncoder.encode(utilisateur.getMotdepasse()));
        utilisateur.setXp(utilisateur.getXp() != null ? utilisateur.getXp() : 0);
        utilisateur.setHp(utilisateur.getHp() != null ? utilisateur.getHp() : 100);

        return utilisateurRepository.save(utilisateur);
    }

    public Utilisateur trouverParUsername(String username) {
        return utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));
    }

    public Utilisateur connecter(String username, String motdepasse) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, motdepasse));

        return trouverParUsername(authentication.getName());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = trouverParUsername(username);

        return User.withUsername(utilisateur.getUsername())
                .password(utilisateur.getMotdepasse())
                .authorities(AuthorityUtils.createAuthorityList("ROLE_USER"))
                .build();
    }
}

package com.jocf.sporttrack.config;

import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import com.jocf.sporttrack.web.SessionKeys;
import com.jocf.sporttrack.web.SessionUtilisateur;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class SessionAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UtilisateurService utilisateurService;

    public SessionAuthenticationSuccessHandler(UtilisateurService utilisateurService) {
        this.utilisateurService = utilisateurService;
        setDefaultTargetUrl("/home");
        setAlwaysUseDefaultTargetUrl(true);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        Utilisateur u = utilisateurService.trouverParEmail(authentication.getName());
        var session = request.getSession();
        session.setAttribute(SessionKeys.UTILISATEUR_ID, u.getId());
        session.setAttribute(SessionKeys.UTILISATEUR, SessionUtilisateur.from(u));

        if (u.getTypeUtilisateur() == TypeUtilisateur.ADMIN) {
            getRedirectStrategy().sendRedirect(request, response, "/homeAdmin");
        } else {
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}

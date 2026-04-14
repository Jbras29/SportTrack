package com.jocf.sporttrack.controller;

import com.jocf.sporttrack.dto.CreerCompteRequest;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.service.UtilisateurService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private UtilisateurService utilisateurService;

    @InjectMocks
    private LoginController controller;

    @Test
    void loginPage_retourneVue() {
        assertThat(controller.loginPage()).isEqualTo("login");
    }

    @Test
    void registerPage_retourneVue() {
        assertThat(controller.registerPage()).isEqualTo("/account/register");
    }

    @Test
    void creerCompte_redirigeVersLoginQuandSucces() {
        CreerCompteRequest form = new CreerCompteRequest();
        form.setNom("Doe");
        form.setPrenom("Jane");
        form.setEmail("jane@test.com");
        form.setMotdepasse("secret");
        form.setSexe(" ");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.creerCompte(form, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/login");

        ArgumentCaptor<Utilisateur> captor = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurService).creerUtilisateur(captor.capture());
        assertThat(captor.getValue().getSexe()).isNull();
        assertThat(captor.getValue().getEmail()).isEqualTo("jane@test.com");
    }

    @Test
    void creerCompte_redirigeVersRegisterQuandErreur() {
        CreerCompteRequest form = new CreerCompteRequest();
        form.setNom("Doe");
        doThrow(new RuntimeException("boom")).when(utilisateurService).creerUtilisateur(org.mockito.ArgumentMatchers.any(Utilisateur.class));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.creerCompte(form, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/register");
        assertThat(redirectAttributes.getFlashAttributes().get("error")).isEqualTo("Erreur : boom");
    }
}

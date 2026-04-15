package com.jocf.sporttrack.service;

import com.jocf.sporttrack.dto.LigneClassementChallenge;
import com.jocf.sporttrack.model.Challenge;
import com.jocf.sporttrack.model.ChallengeSaisieQuotidienne;
import com.jocf.sporttrack.enumeration.TypeUtilisateur;
import com.jocf.sporttrack.model.Utilisateur;
import com.jocf.sporttrack.repository.ChallengeSaisieQuotidienneRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeClassementServiceTest {

    @Mock
    private ChallengeSaisieQuotidienneRepository saisieRepository;

    @InjectMocks
    private ChallengeClassementService service;

    @Test
    void getClassementPourChallenge_agregeLesScores() {
        Utilisateur u1 = Utilisateur.builder().id(1L).nom("A").prenom("A").email("a@test.com").motdepasse("x").typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
        Utilisateur u2 = Utilisateur.builder().id(2L).nom("B").prenom("B").email("b@test.com").motdepasse("x").typeUtilisateur(TypeUtilisateur.UTILISATEUR).build();
        Challenge challenge = Challenge.builder().participants(Set.of(u1, u2)).build();
        when(saisieRepository.findByChallengeAndRealiseTrue(challenge)).thenReturn(List.of(
                ChallengeSaisieQuotidienne.builder().challenge(challenge).utilisateur(u1).realise(true).build(),
                ChallengeSaisieQuotidienne.builder().challenge(challenge).utilisateur(u1).realise(true).build()
        ));

        List<LigneClassementChallenge> classement = service.getClassementPourChallenge(challenge);

        assertThat(classement.get(0).getUtilisateur()).isEqualTo(u1);
        assertThat(classement.get(0).getScore()).isEqualTo(2L);
    }
}

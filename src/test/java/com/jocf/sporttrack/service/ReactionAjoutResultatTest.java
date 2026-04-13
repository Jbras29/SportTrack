package com.jocf.sporttrack.service;

import com.jocf.sporttrack.model.Commentaire;
import com.jocf.sporttrack.model.TypeCommentaire;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReactionAjoutResultatTest {

    @Test
    void record_conserveCommentaireEtDrapeauNouvellementCree() {
        Commentaire c = Commentaire.builder()
                .id(1L)
                .type(TypeCommentaire.REACTION)
                .message("👍")
                .dateCreation(LocalDateTime.now())
                .build();

        ReactionAjoutResultat r = new ReactionAjoutResultat(c, true);

        assertThat(r.commentaire()).isSameAs(c);
        assertThat(r.nouvellementCree()).isTrue();
    }

    @Test
    void record_egaliteSurComposants() {
        Commentaire c = Commentaire.builder()
                .id(2L)
                .type(TypeCommentaire.REACTION)
                .message("❤️")
                .dateCreation(LocalDateTime.of(2025, 1, 1, 12, 0))
                .build();

        ReactionAjoutResultat a = new ReactionAjoutResultat(c, false);
        ReactionAjoutResultat b = new ReactionAjoutResultat(c, false);

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }
}

package com.jocf.sporttrack.config;

import com.jocf.sporttrack.model.Badge;
import com.jocf.sporttrack.repository.BadgeRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadgeDataInitializerTest {

    @Mock
    private BadgeRepository badgeRepository;

    @InjectMocks
    private BadgeDataInitializer initializer;

    @Test
    void run_creeLesBadgesManquantsEtMetAJourLesBadgesExistants() {
        Map<String, Badge> store = new HashMap<>();
        store.put("PREMIER_PAS", new Badge(
                "PREMIER_PAS",
                "Premier pas",
                "https://placehold.co/120x120/059669/ffffff/png?text=1er",
                "Première activité enregistrée."));
        store.put("CINQ_K_STARTER", new Badge(
                "CINQ_K_STARTER",
                "Ancien nom",
                "ancienne-photo",
                "ancienne description"));

        when(badgeRepository.findByCode(anyString()))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));
        when(badgeRepository.save(any(Badge.class)))
                .thenAnswer(invocation -> {
                    Badge badge = invocation.getArgument(0);
                    store.put(badge.getCode(), badge);
                    return badge;
                });

        initializer.run();

        ArgumentCaptor<Badge> captor = ArgumentCaptor.forClass(Badge.class);
        verify(badgeRepository, atLeastOnce()).save(captor.capture());

        List<Badge> sauvegardes = captor.getAllValues();
        assertThat(sauvegardes).noneMatch(badge -> "PREMIER_PAS".equals(badge.getCode()));
        assertThat(sauvegardes).anySatisfy(badge -> {
            assertThat(badge.getCode()).isEqualTo("CINQ_K_STARTER");
            assertThat(badge.getNom()).isEqualTo("5K Starter");
            assertThat(badge.getPhoto())
                    .isEqualTo("https://placehold.co/120x120/2563eb/ffffff/png?text=5K");
            assertThat(badge.getDescription()).isEqualTo("Premier parcours de 5 km.");
        });
        assertThat(store).hasSize(20);
        assertThat(store.get("CINQ_K_STARTER").getNom()).isEqualTo("5K Starter");
    }
}

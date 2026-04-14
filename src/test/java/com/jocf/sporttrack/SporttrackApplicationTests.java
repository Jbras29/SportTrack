package com.jocf.sporttrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;

class SporttrackApplicationTests {

    @Test
    void annotationsPrincipales_sontPresentes() {
        assertThat(SporttrackApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(SporttrackApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
    }
}

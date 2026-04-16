package com.jocf.sporttrack;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SporttrackApplicationTests {

    @Test
    void annotationsPrincipales_sontPresentes() {
        assertThat(SporttrackApplication.class.isAnnotationPresent(SpringBootApplication.class)).isTrue();
        assertThat(SporttrackApplication.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
    }

    @Test
    void methodeMain_estExposee() throws NoSuchMethodException {
        assertThat(SporttrackApplication.class.getDeclaredMethod("main", String[].class)).isNotNull();
    }

    @Test
    void main_launchApplication() {
        new SporttrackApplication();
        assertThatCode(() -> SporttrackApplication.main(new String[] {"--spring.main.web-application-type=none"}))
                .doesNotThrowAnyException();
    }
}

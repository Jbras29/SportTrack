package com.jocf.sporttrack.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class NavigationModelAdviceTest {

    private final NavigationModelAdvice advice = new NavigationModelAdvice();

    @Test
    void navRequestPath_retourneUriSansContexte() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/app");
        request.setRequestURI("/app/profile/12");

        assertThat(advice.navRequestPath(request)).isEqualTo("/profile/12");
    }

    @Test
    void navRequestPath_retourneSlashQuandUriVide() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("");
        request.setRequestURI("");

        assertThat(advice.navRequestPath(request)).isEqualTo("/");
    }
}

package com.jocf.sporttrack.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void navRequestPath_retourneUriQuandLeContexteNeCorrespondPas() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContextPath("/app");
        request.setRequestURI("/profile/12");

        assertThat(advice.navRequestPath(request)).isEqualTo("/profile/12");
    }

    @Test
    void navRequestPath_retourneSlashQuandUriEstNulle() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getContextPath()).thenReturn("");
        when(request.getRequestURI()).thenReturn(null);

        assertThat(advice.navRequestPath(request)).isEqualTo("/");
    }
}

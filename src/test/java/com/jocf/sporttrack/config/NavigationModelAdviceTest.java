package com.jocf.sporttrack.config;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

class NavigationModelAdviceTest {

    private final NavigationModelAdvice advice = new NavigationModelAdvice();

    @ParameterizedTest(name = "{0}")
    @MethodSource("navRequestPathCases")
    void navRequestPath_gereLesPrincipauxCas(String name, HttpServletRequest request, String expected) {
        assertThat(advice.navRequestPath(request)).isEqualTo(expected);
    }

    private static Stream<Arguments> navRequestPathCases() {
        MockHttpServletRequest requestAvecContexte = new MockHttpServletRequest();
        requestAvecContexte.setContextPath("/app");
        requestAvecContexte.setRequestURI("/app/profile/12");

        MockHttpServletRequest requestVide = new MockHttpServletRequest();
        requestVide.setContextPath("");
        requestVide.setRequestURI("");

        MockHttpServletRequest requestSansCorrespondance = new MockHttpServletRequest();
        requestSansCorrespondance.setContextPath("/app");
        requestSansCorrespondance.setRequestURI("/profile/12");

        HttpServletRequest requestNull = mock(HttpServletRequest.class);
        when(requestNull.getContextPath()).thenReturn("");
        when(requestNull.getRequestURI()).thenReturn(null);

        return Stream.of(
                Arguments.of("avec contexte", requestAvecContexte, "/profile/12"),
                Arguments.of("uri vide", requestVide, "/"),
                Arguments.of("contexte sans correspondance", requestSansCorrespondance, "/profile/12"),
                Arguments.of("uri nulle", requestNull, "/"));
    }
}

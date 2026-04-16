package com.jocf.sporttrack.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest {

    private final SecurityConfig config = new SecurityConfig();
    private final SessionAuthenticationSuccessHandler successHandler = org.mockito.Mockito.mock(
            SessionAuthenticationSuccessHandler.class);

    @Test
    void passwordEncoder_estUnBCryptPasswordEncoder() {
        PasswordEncoder passwordEncoder = config.passwordEncoder();

        assertThat(passwordEncoder).isInstanceOf(BCryptPasswordEncoder.class);
        assertThat(passwordEncoder.matches("secret", passwordEncoder.encode("secret"))).isTrue();
    }

    @Test
    void securityFilterChain_laissePasserLesRoutesPubliquesEtRedirigeLesProtegees() throws Exception {
        MockMvc mockMvc = construireMockMvcAvecLaConfigurationDeSecurite();

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string("home"));

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string("login"));

        mockMvc.perform(get("/private"))
                .andExpect(status().isUnauthorized());
    }

    private MockMvc construireMockMvcAvecLaConfigurationDeSecurite() throws Exception {
        ObjectPostProcessor<Object> objectPostProcessor = new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                return object;
            }
        };
        AuthenticationManagerBuilder authenticationManagerBuilder =
                new AuthenticationManagerBuilder(objectPostProcessor);
        Map<Class<?>, Object> sharedObjects = new HashMap<>();
        GenericApplicationContext applicationContext = new GenericApplicationContext();
        applicationContext.refresh();
        sharedObjects.put(org.springframework.context.ApplicationContext.class, applicationContext);
        sharedObjects.put(PathPatternRequestMatcher.Builder.class, PathPatternRequestMatcher.withDefaults());
        sharedObjects.put(AuthenticationManager.class, org.mockito.Mockito.mock(AuthenticationManager.class));

        HttpSecurity http = new HttpSecurity(objectPostProcessor, authenticationManagerBuilder, sharedObjects);
        SecurityFilterChain securityFilterChain = config.securityFilterChain(http, successHandler);
        FilterChainProxy filterChainProxy = new FilterChainProxy(List.of(securityFilterChain));
        filterChainProxy.afterPropertiesSet();

        return MockMvcBuilders.standaloneSetup(new TestController())
                .addFilters(filterChainProxy)
                .build();
    }

    @RestController
    static class TestController {
        @GetMapping("/")
        String home() {
            return "home";
        }

        @GetMapping("/login")
        String login() {
            return "login";
        }

        @GetMapping("/private")
        String privateEndpoint() {
            return "private";
        }
    }
}

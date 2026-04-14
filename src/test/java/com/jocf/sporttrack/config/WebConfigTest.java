package com.jocf.sporttrack.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigTest {

    @Test
    void constructeurNormaliseLeCheminEtEnregistreLePatternDesUploads() throws IOException {
        Path dossierTemporaire = Files.createTempDirectory("sporttrack-upload");
        Path cheminBrut = dossierTemporaire.resolve("profiles").resolve("..").resolve("avatars");

        WebConfig config = new WebConfig(cheminBrut.toString());

        assertThat(ReflectionTestUtils.getField(config, "uploadRootAbsoluFileUrl"))
                .isEqualTo("file:" + cheminBrut.toAbsolutePath().normalize() + "/");

        ResourceHandlerRegistry registry =
                new ResourceHandlerRegistry(new GenericApplicationContext(), new MockServletContext());
        config.addResourceHandlers(registry);

        assertThat(registry.hasMappingForPattern("/uploads/profiles/**")).isTrue();
    }
}

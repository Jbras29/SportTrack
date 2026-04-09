package com.jocf.sporttrack.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class CommentaireFeedbackResponseJsonTest {

    @Autowired
    private JsonMapper jsonMapper;

    @Test
    void serialiseLesClesAttenduesParLeFrontend() throws Exception {
        CommentaireFeedbackResponse r = CommentaireFeedbackResponse.ok("Commentaire publié.", null);
        String json = jsonMapper.writeValueAsString(r);
        assertThat(json).contains("\"success\"");
        assertThat(json).contains("\"message\"");
        assertThat(json).contains("\"commentaireId\"");
        assertThat(json).contains("\"commentaire\"");
    }
}

package com.jocf.sporttrack.service;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenMeteoServiceTest {

    @Test
    void getWeatherForLocationAndDate_retourneNullPourParametresInvalides() {
        OpenMeteoService service = new OpenMeteoService();

        assertThat(service.getWeatherForLocationAndDate(null, LocalDate.now())).isNull();
        assertThat(service.getWeatherForLocationAndDate("Paris", null)).isNull();
    }

    @Test
    void getWeatherForLocationAndDate_retourneNullQuandGeocodageVide() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

        assertThat(service.getWeatherForLocationAndDate("Paris", LocalDate.now())).isNull();
        server.verify();
    }

    @Test
    void getWeatherForLocationAndDate_retourneInfosQuandReponseValide() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("{\"results\":[{\"latitude\":48.85,\"longitude\":2.35}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("api.open-meteo.com")))
                .andRespond(withSuccess("""
                        {"daily":{
                          "weathercode":[61],
                          "temperature_2m_max":[20.0],
                          "temperature_2m_min":[10.0]
                        }}
                        """, MediaType.APPLICATION_JSON));

        OpenMeteoService.WeatherInfo info =
                service.getWeatherForLocationAndDate("Paris", LocalDate.now().plusDays(1));

        assertThat(info).isEqualTo(new OpenMeteoService.WeatherInfo(15.0, "Pluie"));
        server.verify();
    }

    @Test
    void getWeatherForLocationAndDate_retourneNullQuandLaReponseNeContientPasDeBlocDaily() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("{\"results\":[{\"latitude\":48.85,\"longitude\":2.35}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("api.open-meteo.com")))
                .andRespond(withSuccess("{\"foo\":{}}", MediaType.APPLICATION_JSON));

        assertThat(service.getWeatherForLocationAndDate("Paris", LocalDate.now().plusDays(1))).isNull();
        server.verify();
    }

    @Test
    void getWeatherForLocationAndDate_utiliseLArchiveEtRetourneUnLabelInconnu() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("{\"results\":[{\"latitude\":48.85,\"longitude\":2.35}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("archive-api.open-meteo.com")))
                .andRespond(withSuccess("""
                        {"daily":{
                          "weathercode":[999],
                          "temperature_2m_max":[14.0],
                          "temperature_2m_min":[10.0]
                        }}
                        """, MediaType.APPLICATION_JSON));

        OpenMeteoService.WeatherInfo info =
                service.getWeatherForLocationAndDate("Paris", LocalDate.now().minusDays(1));

        assertThat(info).isEqualTo(new OpenMeteoService.WeatherInfo(12.0, "Inconnu"));
        server.verify();
    }
}

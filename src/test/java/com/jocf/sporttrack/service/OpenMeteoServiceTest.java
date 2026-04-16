package com.jocf.sporttrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.lang.reflect.Method;
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
    void suggestLocations_etLocationExists_retournentVidePourParametresInvalides() {
        OpenMeteoService service = new OpenMeteoService();

        assertThat(service.suggestLocations(null, 3)).isEmpty();
        assertThat(service.suggestLocations("   ", 3)).isEmpty();
        assertThat(service.suggestLocations("Pa", 0)).isEmpty();
        assertThat(service.locationExists(null)).isFalse();
        assertThat(service.locationExists("   ")).isFalse();
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
    void suggestLocations_retourneVideQuandLaReponseEstVide() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

        assertThat(service.suggestLocations("Pa", 3)).isEmpty();
        server.verify();
    }

    @Test
    void suggestLocations_retourneLesVillesTrouvees() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"name":"Paris"},
                          {"name":"Paray-Vieille-Poste"},
                          {"name":"Pau"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.suggestLocations("Pa", 2)).containsExactly("Paris", "Paray-Vieille-Poste");
        server.verify();
    }

    @Test
    void locationExists_valideUniquementLesVillesExactes() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("""
                        {"results":[
                          {"name":"Paris"},
                          {"name":"Paray-Vieille-Poste"}
                        ]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.locationExists("Paris")).isTrue();
        server.verify();
    }

    @Test
    void getWeatherForLocationAndDate_retourneNullQuandLaReponseDailyEstVide() {
        OpenMeteoService service = new OpenMeteoService();
        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        server.expect(requestTo(containsString("geocoding-api.open-meteo.com")))
                .andRespond(withSuccess("{\"results\":[{\"latitude\":48.85,\"longitude\":2.35}]}", MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("api.open-meteo.com")))
                .andRespond(withSuccess("""
                        {"daily":{
                          "weathercode":[],
                          "temperature_2m_max":[],
                          "temperature_2m_min":[]
                        }}
                        """, MediaType.APPLICATION_JSON));

        assertThat(service.getWeatherForLocationAndDate("Paris", LocalDate.now().plusDays(1))).isNull();
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

    @Test
    void weatherLabelFromCode_couvreTousLesGroupes() throws Exception {
        assertThat(invokeLabel(0)).isEqualTo("Ensoleillé");
        assertThat(invokeLabel(2)).isEqualTo("Nuageux");
        assertThat(invokeLabel(45)).isEqualTo("Brouillard");
        assertThat(invokeLabel(51)).isEqualTo("Bruine");
        assertThat(invokeLabel(61)).isEqualTo("Pluie");
        assertThat(invokeLabel(71)).isEqualTo("Neige");
        assertThat(invokeLabel(80)).isEqualTo("Averses de pluie");
        assertThat(invokeLabel(85)).isEqualTo("Averses de neige");
        assertThat(invokeLabel(95)).isEqualTo("Orage");
        assertThat(invokeLabel(999)).isEqualTo("Inconnu");
    }

    @Test
    void buildWeatherInfo_retourneNullQuandLesTableauxSontVides() throws Exception {
        OpenMeteoService service = new OpenMeteoService();
        JsonNode dailyNode = new ObjectMapper().readTree("""
                {"temperature_2m_max":[],"temperature_2m_min":[],"weathercode":[]}
                """);

        assertThat(invokeBuildWeatherInfo(service, LocalDate.now(), dailyNode)).isNull();
    }

    private static String invokeLabel(int code) throws Exception {
        Method method = OpenMeteoService.class.getDeclaredMethod("weatherLabelFromCode", int.class);
        method.setAccessible(true);
        return (String) method.invoke(null, code);
    }

    private static OpenMeteoService.WeatherInfo invokeBuildWeatherInfo(OpenMeteoService service, LocalDate date, JsonNode dailyNode)
            throws Exception {
        Method method = OpenMeteoService.class.getDeclaredMethod("buildWeatherInfo", LocalDate.class, JsonNode.class);
        method.setAccessible(true);
        return (OpenMeteoService.WeatherInfo) method.invoke(service, date, dailyNode);
    }
}

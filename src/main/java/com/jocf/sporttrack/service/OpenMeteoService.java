package com.jocf.sporttrack.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class OpenMeteoService {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoService.class);
    private static final String JSON_RESULTS = "results";
    private static final String JSON_DAILY = "daily";
    /** Nom du paramètre de requête API (séries temporelles journalières). */
    private static final String QUERY_PARAM_DAILY = "daily";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record WeatherInfo(Double temperature, String condition) {}

    public List<String> suggestLocations(String query, int limit) {
        if (query == null || query.trim().isEmpty() || limit <= 0) {
            return List.of();
        }

        try {
            String geoUrl = UriComponentsBuilder.fromUriString("https://geocoding-api.open-meteo.com/v1/search")
                    .queryParam("name", query.trim())
                    .queryParam("count", Math.max(1, limit))
                    .queryParam("language", "fr")
                    .queryParam("format", "json")
                    .toUriString();
            String geoResponseStr = restTemplate.getForObject(geoUrl, String.class);
            if (geoResponseStr == null || geoResponseStr.isBlank()) {
                return List.of();
            }

            JsonNode geoNode = objectMapper.readTree(geoResponseStr);
            if (!geoNode.has(JSON_RESULTS) || geoNode.get(JSON_RESULTS).isEmpty()) {
                return List.of();
            }

            LinkedHashSet<String> suggestions = new LinkedHashSet<>();
            for (JsonNode resultNode : geoNode.get(JSON_RESULTS)) {
                JsonNode nameNode = resultNode.get("name");
                if (nameNode == null) {
                    continue;
                }
                String name = nameNode.asText().trim();
                if (!name.isEmpty()) {
                    suggestions.add(name);
                }
                if (suggestions.size() >= limit) {
                    break;
                }
            }
            return new ArrayList<>(suggestions);
        } catch (Exception e) {
            log.warn("Erreur OpenMeteoService (suggestions) : {}", e.getMessage());
            return List.of();
        }
    }

    public boolean locationExists(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }

        String saisie = location.trim();
        return suggestLocations(saisie, 20).stream()
                .map(String::trim)
                .anyMatch(suggestion -> suggestion.equalsIgnoreCase(saisie));
    }

    public WeatherInfo getWeatherForLocationAndDate(String location, LocalDate date) {
        if (location == null || location.trim().isEmpty() || date == null) {
            return null;
        }
        try {
            GeoCoords coords = resolveGeo(location);
            if (coords == null) {
                return null;
            }
            JsonNode weatherNode = fetchWeatherRoot(coords, date);
            if (weatherNode == null || !weatherNode.has(JSON_DAILY)) {
                log.warn("OpenMeteoService : pas de données '{}' dans la réponse.", JSON_DAILY);
                return null;
            }
            return buildWeatherInfo(location, date, weatherNode.get(JSON_DAILY));
        } catch (Exception e) {
            log.warn("Erreur OpenMeteoService : {}", e.getMessage());
            return null;
        }
    }

    private GeoCoords resolveGeo(String location) throws com.fasterxml.jackson.core.JsonProcessingException {
        String geoUrl = UriComponentsBuilder.fromUriString("https://geocoding-api.open-meteo.com/v1/search")
                .queryParam("name", location)
                .queryParam("count", 1)
                .queryParam("language", "fr")
                .queryParam("format", "json")
                .toUriString();
        String geoResponseStr = restTemplate.getForObject(geoUrl, String.class);
        JsonNode geoNode = objectMapper.readTree(geoResponseStr);
        if (!geoNode.has(JSON_RESULTS) || geoNode.get(JSON_RESULTS).size() == 0) {
            log.warn("OpenMeteoService : aucun résultat de géocodage pour la recherche saisie");
            return null;
        }
        JsonNode resultNode = geoNode.get(JSON_RESULTS).get(0);
        return new GeoCoords(resultNode.get("latitude").asDouble(), resultNode.get("longitude").asDouble());
    }

    private JsonNode fetchWeatherRoot(GeoCoords coords, LocalDate date) throws com.fasterxml.jackson.core.JsonProcessingException {
        String dateStr = date.toString();
        LocalDate today = LocalDate.now();
        String baseUrl = date.isBefore(today)
                ? "https://archive-api.open-meteo.com/v1/archive"
                : "https://api.open-meteo.com/v1/forecast";
        String weatherUrl = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("latitude", coords.lat())
                .queryParam("longitude", coords.lon())
                .queryParam(QUERY_PARAM_DAILY, "weathercode,temperature_2m_max,temperature_2m_min")
                .queryParam("timezone", "auto")
                .queryParam("start_date", dateStr)
                .queryParam("end_date", dateStr)
                .toUriString();
        String weatherResponseStr = restTemplate.getForObject(weatherUrl, String.class);
        return objectMapper.readTree(weatherResponseStr);
    }

    private WeatherInfo buildWeatherInfo(String location, LocalDate date, JsonNode dailyNode) {
        String dateStr = date.toString();
        if (dailyNode.get("temperature_2m_max").size() == 0) {
            log.warn("OpenMeteoService : données météo vides pour la date {}", dateStr);
            return null;
        }
        double tMax = dailyNode.get("temperature_2m_max").get(0).asDouble();
        double tMin = dailyNode.get("temperature_2m_min").get(0).asDouble();
        int weatherCode = dailyNode.get("weathercode").get(0).asInt();
        double avgTemp = (tMax + tMin) / 2.0;
        String condition = weatherLabelFromCode(weatherCode);
        log.debug("OpenMeteoService OK : date {} → {} {}°C", dateStr, condition, avgTemp);
        return new WeatherInfo(Math.round(avgTemp * 10.0) / 10.0, condition);
    }

    private static String weatherLabelFromCode(int code) {
        return switch (code) {
            case 0 -> "Ensoleillé";
            case 1, 2, 3 -> "Nuageux";
            case 45, 48 -> "Brouillard";
            case 51, 52, 53, 54, 55, 56, 57 -> "Bruine";
            case 61, 62, 63, 64, 65, 66, 67 -> "Pluie";
            case 71, 72, 73, 74, 75, 76, 77 -> "Neige";
            case 80, 81, 82 -> "Averses de pluie";
            case 85, 86 -> "Averses de neige";
            case 95, 96, 97, 98, 99 -> "Orage";
            default -> "Inconnu";
        };
    }

    private record GeoCoords(double lat, double lon) {}
}

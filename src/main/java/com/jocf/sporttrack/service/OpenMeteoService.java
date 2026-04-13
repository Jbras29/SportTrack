package com.jocf.sporttrack.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;

@Service
public class OpenMeteoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenMeteoService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public static class WeatherInfo {
        public Double temperature;
        public String condition;
        
        public WeatherInfo(Double temperature, String condition) {
            this.temperature = temperature;
            this.condition = condition;
        }
    }

    public WeatherInfo getWeatherForLocationAndDate(String location, LocalDate date) {
        if (location == null || location.trim().isEmpty() || date == null) {
            return null;
        }

        try {
            // 1. Geocoding
            String geoUrl = UriComponentsBuilder.fromUriString("https://geocoding-api.open-meteo.com/v1/search")
                    .queryParam("name", location)
                    .queryParam("count", 1)
                    .queryParam("language", "fr")
                    .queryParam("format", "json")
                    .toUriString();

            String geoResponseStr = restTemplate.getForObject(geoUrl, String.class);
            JsonNode geoNode = objectMapper.readTree(geoResponseStr);

            if (!geoNode.has("results") || geoNode.get("results").size() == 0) {
                return null;
            }

            JsonNode resultNode = geoNode.get("results").get(0);
            double lat = resultNode.get("latitude").asDouble();
            double lon = resultNode.get("longitude").asDouble();

            // 2. Weather
            String dateStr = date.toString();
            String weatherUrl = UriComponentsBuilder.fromUriString("https://api.open-meteo.com/v1/forecast")
                    .queryParam("latitude", lat)
                    .queryParam("longitude", lon)
                    .queryParam("daily", "weathercode,temperature_2m_max,temperature_2m_min")
                    .queryParam("timezone", "auto")
                    .queryParam("start_date", dateStr)
                    .queryParam("end_date", dateStr)
                    .toUriString();

            String weatherResponseStr = restTemplate.getForObject(weatherUrl, String.class);
            JsonNode weatherNode = objectMapper.readTree(weatherResponseStr);

            if (!weatherNode.has("daily")) {
                return null; // Could not fetch daily forecast
            }
            JsonNode dailyNode = weatherNode.get("daily");

            double tMax = dailyNode.get("temperature_2m_max").get(0).asDouble();
            double tMin = dailyNode.get("temperature_2m_min").get(0).asDouble();
            int weatherCode = dailyNode.get("weathercode").get(0).asInt();

            double avgTemp = (tMax + tMin) / 2.0;

            String condition = getWeatherConditionFromCode(weatherCode);

            return new WeatherInfo(Math.round(avgTemp * 10.0) / 10.0, condition);

        } catch (Exception e) {
            // En cas d'erreur (réseau, pas de donnée...), on log discrètement et on retourne null.
            System.err.println("Erreur OpenMeteoService : " + e.getMessage());
            return null;
        }
    }

    private String getWeatherConditionFromCode(int code) {
        if (code == 0) return "Ensoleillé";
        if (code >= 1 && code <= 3) return "Nuageux";
        if (code == 45 || code == 48) return "Brouillard";
        if (code >= 51 && code <= 57) return "Bruine";
        if (code >= 61 && code <= 67) return "Pluie";
        if (code >= 71 && code <= 77) return "Neige";
        if (code >= 80 && code <= 82) return "Averses de pluie";
        if (code >= 85 && code <= 86) return "Averses de neige";
        if (code >= 95 && code <= 99) return "Orage";
        return "Inconnu";
    }
}

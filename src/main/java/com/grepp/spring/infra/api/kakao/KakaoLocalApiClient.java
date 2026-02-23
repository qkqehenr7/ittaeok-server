package com.grepp.spring.infra.api.kakao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class KakaoLocalApiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kakao.middle-location.api-key}")
    private String kakaoApiKey;

    public List<JsonNode> findNearestStations(double latitude, double longitude) throws JsonProcessingException {
        String url = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/category.json")
                .queryParam("category_group_code", "SW8")
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .queryParam("radius", 3000)
                .queryParam("sort", "distance")
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        JsonNode json = objectMapper.readTree(response.getBody());
        JsonNode documents = json.get("documents");

        List<JsonNode> result = new ArrayList<>();
        String stationName = "";

        for (JsonNode doc : documents) {
            String fullName = doc.get("place_name").asText();
            String sn = fullName.split(" ")[0];

            if (!stationName.equals(sn)) {
                stationName = sn;
                result.add(doc);
            }
            if (result.size() >= 3)
                break;
        }

        return result;
    }
}

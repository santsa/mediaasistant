package com.santsa.mediaassistant.service;

import java.util.List;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.santsa.mediaassistant.dto.DrugInfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class DrugInfoServiceImpl implements DrugInfoService {

    private final RestClient openFdaClient;
    private static final int MAX_FIELD_LENGTH = 500;

    @Override
    public DrugInfo getDrugInfo(String drugName) {
        log.info("Fetching drug information for: {}", drugName);
        var response = fetchFromOpenFda(drugName);
        return mapToDrugInfo(response, drugName);
    }

    private String extractFirst(Map<?, ?> map, String key) {
        Object value = map.get(key);
        if (value instanceof List<?> list && !list.isEmpty()) {
            var valueStr = list.get(0).toString();
            return valueStr.length() > MAX_FIELD_LENGTH ? valueStr.substring(0, MAX_FIELD_LENGTH) + "..." : valueStr;
        }
        return "No available information";
    }

    private Map<String, Object> fetchFromOpenFda(String drugName) {
        return openFdaClient.get()
                .uri("/drug/label.json?search=openfda.generic_name:{drugName}&limit=1", drugName)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private DrugInfo mapToDrugInfo(Map<String, Object> response, String drugName) {
        if (!(response.get("results") instanceof List<?> results) || response.isEmpty()) {
            log.warn("No results found for drug: {}", drugName);
            return null;
        }

        if (!(results.getFirst() instanceof Map<?, ?> firstResult)) {
            log.warn("Unexpected result format for drug: {}", drugName);
            return null;
        }

        var openFda = firstResult.get("openfda") instanceof Map<?, ?> m ? m : Map.of();
        return new DrugInfo(
                extractFirst(openFda, "brand_name"),
                extractFirst(openFda, "generic_name"),
                extractFirst(firstResult, "purpose"),
                extractFirst(firstResult, "warnings"),
                extractFirst(firstResult, "dosage_and_administration"));
    }

}

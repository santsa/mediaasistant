package com.santsa.mediaassistant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.santsa.mediaassistant.dto.DrugInfo;

/**
 * Uses MockRestServiceServer bound to the RestClient: no real call to OpenFDA.
 */
class DrugInfoServiceImplTest {

    private static final String BASE_URL = "https://api.fda.gov";

    private MockRestServiceServer server;
    private DrugInfoServiceImpl service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        service = new DrugInfoServiceImpl(builder.build());
    }

    private void respondWith(String json) {
        server.expect(requestTo(startsWith(BASE_URL + "/drug/label.json")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void getDrugInfo_searchesByGenericNameWithLimitOne() {
        server.expect(requestTo(startsWith(BASE_URL + "/drug/label.json")))
                .andExpect(queryParam("search", "openfda.generic_name:ibuprofen"))
                .andExpect(queryParam("limit", "1"))
                .andRespond(withSuccess("{\"results\":[{}]}", MediaType.APPLICATION_JSON));

        service.getDrugInfo("ibuprofen");

        server.verify();
    }

    @Test
    void getDrugInfo_whenResultFound_mapsFirstValueOfEachField() {
        respondWith("""
                {"results":[{
                  "openfda":{"brand_name":["Advil","Motrin"],"generic_name":["IBUPROFEN"]},
                  "purpose":["Pain reliever"],
                  "warnings":["Stomach bleeding warning"],
                  "dosage_and_administration":["1 tablet every 4 to 6 hours"]
                }]}
                """);

        DrugInfo result = service.getDrugInfo("ibuprofen");

        assertThat(result).isEqualTo(new DrugInfo("Advil", "IBUPROFEN", "Pain reliever",
                "Stomach bleeding warning", "1 tablet every 4 to 6 hours"));
        server.verify();
    }

    @Test
    void getDrugInfo_whenFieldsMissingOrEmpty_returnsNoAvailableInformation() {
        respondWith("""
                {"results":[{"purpose":[]}]}
                """);

        DrugInfo result = service.getDrugInfo("ibuprofen");

        assertThat(result).isEqualTo(new DrugInfo("No available information", "No available information",
                "No available information", "No available information", "No available information"));
    }

    @Test
    void getDrugInfo_whenFieldLongerThan500Chars_truncatesWithEllipsis() {
        String longWarning = "x".repeat(600);
        respondWith("{\"results\":[{\"warnings\":[\"" + longWarning + "\"]}]}");

        DrugInfo result = service.getDrugInfo("ibuprofen");

        assertThat(result.warnings()).hasSize(503).isEqualTo("x".repeat(500) + "...");
    }

    @Test
    void getDrugInfo_whenFieldExactly500Chars_doesNotTruncate() {
        String warning = "x".repeat(500);
        respondWith("{\"results\":[{\"warnings\":[\"" + warning + "\"]}]}");

        assertThat(service.getDrugInfo("ibuprofen").warnings()).isEqualTo(warning);
    }

    @Test
    void getDrugInfo_whenResponseHasNoResults_returnsNull() {
        respondWith("{\"meta\":{}}");

        assertThat(service.getDrugInfo("unknown")).isNull();
    }

    @Test
    void getDrugInfo_whenOpenFdaAnswers404_returnsNullInsteadOfFailing() {
        server.expect(requestTo(startsWith(BASE_URL + "/drug/label.json")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"No matches found!\"}}"));

        assertThat(service.getDrugInfo("notadrug")).isNull();
        server.verify();
    }

    @Test
    void getDrugInfo_whenOpenFdaAnswers500_propagatesRestClientException() {
        // Real outages still reach GlobalExceptionHandler (503 to the API caller).
        server.expect(requestTo(startsWith(BASE_URL + "/drug/label.json")))
                .andRespond(withServerError());

        assertThatThrownBy(() -> service.getDrugInfo("ibuprofen")).isInstanceOf(RestClientException.class);
    }

    @Test
    void getDrugInfo_whenResponseBodyIsEmpty_returnsNull() {
        server.expect(requestTo(startsWith(BASE_URL + "/drug/label.json")))
                .andRespond(withSuccess());

        assertThat(service.getDrugInfo("ibuprofen")).isNull();
    }

    @Test
    void getDrugInfo_whenResultsListIsEmpty_returnsNull() {
        respondWith("{\"results\":[]}");

        assertThat(service.getDrugInfo("ibuprofen")).isNull();
    }

    @Test
    void getDrugInfo_whenFirstResultIsNotAnObject_returnsNull() {
        respondWith("{\"results\":[\"unexpected\"]}");

        assertThat(service.getDrugInfo("ibuprofen")).isNull();
    }
}

package com.pickleball.pickleball.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the HTTP API against a real server. The in-memory database is overridden so this
 * class does not share the schema of the other test context.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.datasource.url=jdbc:h2:mem:apitest;DB_CLOSE_DELAY=-1")
class CourtApiTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @LocalServerPort
    private int port;

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @BeforeEach
    void resetSession() {
        assertEquals(200, post("/courts/reset", null).statusCode());
    }

    @Test
    void rootRedirectsToTheCourtScreen() {
        HttpResponse<String> response = get("/");

        assertEquals(302, response.statusCode());
        assertNotNull(response.headers().firstValue("Location").orElse(null));
        assertTrue(response.headers().firstValue("Location").orElseThrow().endsWith("/courts.html"));
    }

    @Test
    void theStaticUiIsServed() {
        HttpResponse<String> response = get("/courts.html");

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Rotate to Next Matches"));
    }

    @Test
    void addingACourtStoresBothTeamsAndTheReserves() {
        HttpResponse<String> added = post("/courts/add", """
                {"team1":{"player1":"A1","player2":"A2"},
                 "team2":{"player1":"B1","player2":"B2"},
                 "reserves":["R1","R2"]}""");
        assertEquals(200, added.statusCode());

        List<Map<String, Object>> courts = courts();
        assertEquals(1, courts.size());
        assertNotNull(courts.get(0).get("id"));
        assertEquals("A1", team(courts.get(0), "team1").get("player1"));
        assertEquals("B2", team(courts.get(0), "team2").get("player2"));
        assertEquals(List.of("R1", "R2"), courts.get(0).get("reserves"));
        assertEquals(Boolean.FALSE, courts.get(0).get("team1Win"));
        assertEquals(Boolean.FALSE, courts.get(0).get("team2Win"));
    }

    @Test
    void addingACourtWithoutReservesIsAllowed() {
        assertEquals(200, post("/courts/add", """
                {"team1":{"player1":"A1","player2":"A2"},
                 "team2":{"player1":"B1","player2":"B2"}}""").statusCode());

        assertEquals(List.of(), courts().get(0).get("reserves"));
    }

    @Test
    void addingACourtRejectsIncompleteTeams() {
        HttpResponse<String> response = post("/courts/add", """
                {"team1":{"player1":"A1"},
                 "team2":{"player1":"B1","player2":"B2"}}""");

        assertEquals(400, response.statusCode());
        assertProblemDetail(response, "Each team needs player1 and player2");
        assertTrue(courts().isEmpty());
    }

    @Test
    void addingACourtRejectsBlankPlayerNames() {
        HttpResponse<String> response = post("/courts/add", """
                {"team1":{"player1":"A1","player2":"  "},
                 "team2":{"player1":"B1","player2":"B2"}}""");

        assertEquals(400, response.statusCode());
        assertProblemDetail(response, "Player names must be non-empty strings");
        assertTrue(courts().isEmpty());
    }

    @Test
    void addingACourtRejectsReservesThatAreNotAList() {
        HttpResponse<String> response = post("/courts/add", """
                {"team1":{"player1":"A1","player2":"A2"},
                 "team2":{"player1":"B1","player2":"B2"},
                 "reserves":"R1"}""");

        assertEquals(400, response.statusCode());
        assertProblemDetail(response, "reserves must be a list of names");
        assertTrue(courts().isEmpty());
    }

    @Test
    void addingACourtWithAMalformedBodyIsRejected() {
        HttpResponse<String> response = post("/courts/add", "{not json");

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"));
    }

    @Test
    void addingACourtWithAnEmptyBodyIsRejected() {
        HttpResponse<String> response = post("/courts/add", "null");

        assertEquals(400, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"));
        assertTrue(courts().isEmpty());
    }

    @Test
    void deletingACourtRemovesItAndTheSecondDeleteFails() {
        addCourt();
        long id = courtId();

        assertEquals(200, delete("/courts/" + id).statusCode());
        assertTrue(courts().isEmpty());

        HttpResponse<String> second = delete("/courts/" + id);
        assertEquals(404, second.statusCode());
        assertProblemDetail(second, "Court not found");
    }

    @Test
    void deletingAnUnknownCourtFails() {
        assertEquals(404, delete("/courts/42").statusCode());
    }

    @Test
    void recordingAndClearingAWinnerUpdatesTheCourt() {
        addCourt();
        long id = courtId();

        assertEquals(200, put("/courts/" + id + "/winner", "{\"winner\":\"team1\"}").statusCode());
        assertEquals(Boolean.TRUE, courts().get(0).get("team1Win"));
        assertEquals(Boolean.FALSE, courts().get(0).get("team2Win"));

        assertEquals(200, put("/courts/" + id + "/winner", "{\"winner\":\"\"}").statusCode());
        assertEquals(Boolean.FALSE, courts().get(0).get("team1Win"));
        assertEquals(Boolean.FALSE, courts().get(0).get("team2Win"));
    }

    @Test
    void recordingAWinnerIgnoresCaseAndSurroundingSpaces() {
        addCourt();
        long id = courtId();

        assertEquals(200, put("/courts/" + id + "/winner", "{\"winner\":\" TEAM1 \"}").statusCode());

        assertEquals(Boolean.TRUE, courts().get(0).get("team1Win"));
    }

    @Test
    void recordingAWinnerWithoutTheWinnerFieldIsRejected() {
        addCourt();
        long id = courtId();

        HttpResponse<String> response = put("/courts/" + id + "/winner", "{}");

        assertEquals(400, response.statusCode());
        assertProblemDetail(response, "winner must be team1, team2 or an empty string");
        assertEquals(Boolean.FALSE, courts().get(0).get("team1Win"));
    }

    @Test
    void recordingAnUnknownWinnerFails() {
        addCourt();
        long id = courtId();

        HttpResponse<String> response = put("/courts/" + id + "/winner", "{\"winner\":\"team3\"}");

        assertEquals(400, response.statusCode());
        assertProblemDetail(response, "winner must be team1 or team2");
        assertEquals(Boolean.FALSE, courts().get(0).get("team1Win"));
    }

    @Test
    void recordingAWinnerOnAnUnknownCourtFails() {
        HttpResponse<String> response = put("/courts/42/winner", "{\"winner\":\"team1\"}");

        assertEquals(404, response.statusCode());
        assertProblemDetail(response, "Court not found");
    }

    @Test
    void rotatingRequiresAWinnerOnEveryCourt() {
        addCourt();

        HttpResponse<String> response = post("/courts/calculate", null);

        assertEquals(400, response.statusCode());
        assertProblemDetail(response, "Every court needs a winner before rotating");
        assertEquals(1, round());
    }

    @Test
    void rotatingSwapsTheTeamsAndAdvancesTheRound() {
        addCourt();
        long id = courtId();
        assertEquals(200, put("/courts/" + id + "/winner", "{\"winner\":\"team1\"}").statusCode());

        assertEquals(200, post("/courts/calculate", null).statusCode());

        Map<String, Object> court = courts().get(0);
        assertEquals("A1", team(court, "team1").get("player1"));
        assertEquals("B1", team(court, "team1").get("player2"));
        assertEquals("A2", team(court, "team2").get("player1"));
        assertEquals("B2", team(court, "team2").get("player2"));
        assertEquals(Boolean.FALSE, court.get("team1Win"));
        assertEquals(2, round());
    }

    @Test
    void rotatingALadderMovesWinnersUpAndLosersDown() {
        addCourt("A1", "A2", "B1", "B2");
        addCourt("C1", "C2", "D1", "D2");
        addCourt("E1", "E2", "F1", "F2");

        List<Map<String, Object>> before = courts();
        recordWinner(before.get(0), "team1");
        recordWinner(before.get(1), "team2");
        recordWinner(before.get(2), "team1");

        assertEquals(200, post("/courts/calculate", null).statusCode());

        List<Map<String, Object>> after = courts();
        assertEquals("A1", team(after.get(0), "team1").get("player1"));
        assertEquals("D1", team(after.get(0), "team1").get("player2"));
        assertEquals("E1", team(after.get(1), "team1").get("player1"));
        assertEquals("B1", team(after.get(1), "team1").get("player2"));
        assertEquals("F1", team(after.get(2), "team1").get("player1"));
        assertEquals("C1", team(after.get(2), "team1").get("player2"));
        assertEquals(2, round());
    }

    @Test
    void resettingClearsTheCourtsAndTheRound() {
        addCourt();
        long id = courtId();
        assertEquals(200, put("/courts/" + id + "/winner", "{\"winner\":\"team1\"}").statusCode());
        assertEquals(200, post("/courts/calculate", null).statusCode());

        assertEquals(200, post("/courts/reset", null).statusCode());

        assertTrue(courts().isEmpty());
        assertEquals(1, round());
    }

    @Test
    void theRoundEndpointReportsTheCurrentRound() {
        assertEquals(1, round());
    }

    private void addCourt() {
        addCourt("A1", "A2", "B1", "B2");
    }

    private void addCourt(String t1p1, String t1p2, String t2p1, String t2p2) {
        String body = """
                {"team1":{"player1":"%s","player2":"%s"},
                 "team2":{"player1":"%s","player2":"%s"}}""".formatted(t1p1, t1p2, t2p1, t2p2);
        assertEquals(200, post("/courts/add", body).statusCode());
    }

    private void recordWinner(Map<String, Object> court, String team) {
        long id = ((Number) court.get("id")).longValue();
        assertEquals(200, put("/courts/" + id + "/winner", "{\"winner\":\"" + team + "\"}").statusCode());
    }

    private long courtId() {
        return ((Number) courts().get(0).get("id")).longValue();
    }

    private int round() {
        String body = get("/courts/round").body();
        try {
            Map<String, Object> payload = MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {
            });
            return ((Number) payload.get("round")).intValue();
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected round payload: " + body, ex);
        }
    }

    private void assertProblemDetail(HttpResponse<String> response, String detail) {
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"),
                "expected a problem detail response but got: " + response.headers().firstValue("Content-Type").orElse(""));
        try {
            Map<String, Object> problem = MAPPER.readValue(response.body(), new TypeReference<Map<String, Object>>() {
            });
            assertEquals(detail, problem.get("detail"));
            assertEquals(response.statusCode(), ((Number) problem.get("status")).intValue());
            assertNotNull(problem.get("title"));
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected error payload: " + response.body(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> team(Map<String, Object> court, String key) {
        return (Map<String, Object>) court.get(key);
    }

    private List<Map<String, Object>> courts() {
        String body = get("/courts").body();
        try {
            return MAPPER.readValue(body, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("Unexpected courts payload: " + body, ex);
        }
    }

    private HttpResponse<String> get(String path) {
        return send(HttpRequest.newBuilder(uri(path)).GET().build());
    }

    private HttpResponse<String> post(String path, String body) {
        return send(HttpRequest.newBuilder(uri(path))
                .POST(body == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build());
    }

    private HttpResponse<String> put(String path, String body) {
        return send(HttpRequest.newBuilder(uri(path))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build());
    }

    private HttpResponse<String> delete(String path) {
        return send(HttpRequest.newBuilder(uri(path)).DELETE().build());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

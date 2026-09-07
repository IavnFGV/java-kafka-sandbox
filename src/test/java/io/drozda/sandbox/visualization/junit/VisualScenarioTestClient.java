package io.drozda.sandbox.visualization.junit;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class VisualScenarioTestClient {

    private static final Logger log = LoggerFactory.getLogger(VisualScenarioTestClient.class);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(1))
            .build();
    private static final String BASE_URL = "http://localhost:8080/api/scenarios/runtime/session";

    private VisualScenarioTestClient() {
    }

    public static void start(String scenarioId, String testName) {
        postJson(BASE_URL + "/start",
                "{\"scenarioId\":\"" + scenarioId + "\",\"testName\":\"" + escape(testName) + "\"}");
    }

    public static void step(String scenarioId, int stepIndex) {
        postJson(BASE_URL + "/step",
                "{\"scenarioId\":\"" + scenarioId + "\",\"stepIndex\":" + stepIndex + "}");
    }

    public static void event(String scenarioId, String type, String nodeId, String edgeId, String label,
            String fromNodeId, String toNodeId, String status) {
        postJson("http://localhost:8080/api/scenarios/runtime/event",
                "{"
                        + "\"scenarioId\":\"" + scenarioId + "\","
                        + "\"type\":\"" + escape(type) + "\","
                        + "\"nodeId\":" + jsonString(nodeId) + ","
                        + "\"edgeId\":" + jsonString(edgeId) + ","
                        + "\"label\":" + jsonString(label) + ","
                        + "\"fromNodeId\":" + jsonString(fromNodeId) + ","
                        + "\"toNodeId\":" + jsonString(toNodeId) + ","
                        + "\"status\":" + jsonString(status)
                        + "}");
    }

    public static void complete(String scenarioId, String testName) {
        postJson(BASE_URL + "/complete",
                "{\"scenarioId\":\"" + scenarioId + "\",\"testName\":\"" + escape(testName) + "\"}");
    }

    private static void postJson(String url, String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(2))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            CLIENT.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (ConnectException ex) {
            log.debug("Visualizer app is not running on localhost:8080; skipping runtime update");
        } catch (IOException | InterruptedException ex) {
            log.warn("Failed to send runtime update to visualizer: {}", ex.getMessage());
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static String escape(String input) {
        return input.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String jsonString(String value) {
        return value == null ? "null" : "\"" + escape(value) + "\"";
    }
}

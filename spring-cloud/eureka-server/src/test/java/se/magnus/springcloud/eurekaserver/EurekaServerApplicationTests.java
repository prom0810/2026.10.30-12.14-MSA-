package se.magnus.springcloud.eurekaserver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT,
    properties = "eureka.server.wait-time-in-ms-when-sync-empty=0")
class EurekaServerApplicationTests {
    @LocalServerPort
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Accept", "application/json").GET().build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    @Test
    void catalogLoads() throws Exception {
        var response = get("/eureka/apps");
        assertEquals(200, response.statusCode());
        var applications = JsonMapper.builder().build().readTree(response.body()).path("applications").path("application");
        assertTrue(applications.isArray());
        assertEquals(0, applications.size());
    }

    @Test
    void healthy() throws Exception {
        var response = get("/actuator/health");
        assertEquals(200, response.statusCode());
        assertEquals("UP", JsonMapper.builder().build().readTree(response.body()).path("status").asText());
    }
}

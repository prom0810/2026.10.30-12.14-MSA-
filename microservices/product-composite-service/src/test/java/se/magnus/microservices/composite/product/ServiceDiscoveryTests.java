package se.magnus.microservices.composite.product;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "eureka.client.enabled=false")
@Import(TestChannelBinderConfiguration.class)
class ServiceDiscoveryTests {
    private static final HttpServer server = startServer();

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/product/123", exchange -> {
                byte[] body = "{\"productId\":123,\"name\":\"discovered\",\"weight\":7}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (var output = exchange.getResponseBody()) { output.write(body); }
            });
            server.start();
            return server;
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }

    @DynamicPropertySource
    static void discovery(DynamicPropertyRegistry registry) {
        // Replace Eureka with a deterministic registry; exercise the real load-balanced builder.
        registry.add("spring.cloud.discovery.client.simple.instances.product[0].uri",
            () -> "http://127.0.0.1:" + server.getAddress().getPort());
    }

    @Autowired
    ProductCompositeIntegration integration;

    @Test
    void resolvesServiceNameToRegisteredInstance() {
        var product = integration.getProduct(123).block(Duration.ofSeconds(10));
        assertNotNull(product);
        assertEquals(123, product.getProductId());
        assertEquals("discovered", product.getName());
        assertEquals(7, product.getWeight());
    }

    @AfterAll
    static void stopServer() { server.stop(0); }
}

package se.magnus.microservices.composite.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import se.magnus.api.core.product.Product;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductCompositeIntegrationTests {
    private StreamBridge bridge;
    private ProductCompositeIntegration integration;
    private ClientResponse response;
    private String requestedUrl;

    @BeforeEach
    void setup() {
        bridge = mock(StreamBridge.class);
        WebClient.Builder client = WebClient.builder().exchangeFunction(request -> {
            requestedUrl = request.url().toString();
            return Mono.just(response);
        });
        integration = new ProductCompositeIntegration(Schedulers.immediate(), client,
            JsonMapper.builder().build(), bridge);
    }

    @Test
    void getPreservesErrorMessage() {
        response = ClientResponse.create(HttpStatus.UNPROCESSABLE_CONTENT)
            .header("Content-Type", "application/json").body("{\"message\":\"Invalid productId: -1\"}").build();
        assertEquals("Invalid productId: -1", assertThrows(InvalidInputException.class,
            () -> integration.getProduct(-1).block()).getMessage());
        assertEquals("http://product/product/-1", requestedUrl);
    }

    @Test
    void getTranslatesNotFound() {
        response = ClientResponse.create(HttpStatus.NOT_FOUND)
            .header("Content-Type", "application/json").body("{\"message\":\"missing\"}").build();
        assertEquals("missing", assertThrows(NotFoundException.class,
            () -> integration.getProduct(7).block()).getMessage());
    }

    @Test
    void unexpectedProductErrorIsNotConvertedToSuccess() {
        response = ClientResponse.create(HttpStatus.BAD_REQUEST).body("invalid").build();
        assertThrows(WebClientResponseException.class, () -> integration.getProduct(7).block());
    }

    @Test
    void rejectedPublicationIsNotConvertedToSuccess() {
        when(bridge.send(anyString(), any(Object.class))).thenReturn(false);
        assertThrows(IllegalStateException.class, () -> integration.createProduct(new Product(7,"p",2,null)).block());
        assertThrows(IllegalStateException.class, () -> integration.deleteProduct(7).block());
    }
}

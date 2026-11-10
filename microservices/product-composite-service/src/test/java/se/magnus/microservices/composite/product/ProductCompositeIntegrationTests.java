package se.magnus.microservices.composite.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.magnus.api.core.product.Product;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpMethod.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ProductCompositeIntegrationTests {
    private MockRestServiceServer server;
    private ProductCompositeIntegration integration;

    @BeforeEach
    void setup() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        integration = new ProductCompositeIntegration(restTemplate, JsonMapper.builder().build(),
                "product", 8080, "recommendation", 8080, "review", 8080);
    }

    @Test
    void postUsesBaseUrlAndPreservesErrorMessage() {
        server.expect(requestTo("http://product:8080/product")).andExpect(method(POST))
                .andExpect(content().json("{\"productId\":7,\"name\":\"p\",\"weight\":2}"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Duplicate key, Product Id: 7\"}"));
        InvalidInputException ex = assertThrows(InvalidInputException.class,
                () -> integration.createProduct(new Product(7, "p", 2, null)));
        assertEquals("Duplicate key, Product Id: 7", ex.getMessage());
        server.verify();
    }

    @Test
    void deletesUseCorrectPathsAndQueryParameters() {
        server.expect(requestTo("http://product:8080/product/7")).andExpect(method(DELETE)).andRespond(withSuccess());
        server.expect(requestTo("http://recommendation:8080/recommendation?productId=7"))
                .andExpect(method(DELETE)).andRespond(withSuccess());
        server.expect(requestTo("http://review:8080/review?productId=7"))
                .andExpect(method(DELETE)).andRespond(withSuccess());
        integration.deleteProduct(7);
        integration.deleteRecommendations(7);
        integration.deleteReviews(7);
        server.verify();
    }

    @Test
    void deleteTranslatesNotFound() {
        server.expect(requestTo("http://product:8080/product/7"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"missing\"}"));
        assertEquals("missing", assertThrows(NotFoundException.class, () -> integration.deleteProduct(7)).getMessage());
        server.verify();
    }

    @Test
    void unexpectedClientErrorIsNotConvertedToSuccess() {
        server.expect(requestTo("http://review:8080/review?productId=7"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).body("invalid"));
        assertThrows(HttpClientErrorException.class, () -> integration.deleteReviews(7));
        server.verify();
    }
}

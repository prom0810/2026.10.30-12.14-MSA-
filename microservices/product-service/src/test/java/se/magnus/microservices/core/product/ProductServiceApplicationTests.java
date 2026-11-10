package se.magnus.microservices.core.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;

@SpringBootTest(webEnvironment = RANDOM_PORT,
        properties = "spring.data.mongodb.auto-index-creation=true")
@AutoConfigureWebTestClient
@Testcontainers
class ProductServiceApplicationTests {
    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0.5");

    @Autowired private WebTestClient client;
    @Autowired private ProductRepository repository;

    @BeforeEach
    void setupDb() {
        repository.deleteAll();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void createAndGetProduct() {
        postAndVerifyProduct(1, OK).jsonPath("$.productId").isEqualTo(1);
        getAndVerifyProduct(1, OK)
                .jsonPath("$.productId").isEqualTo(1)
                .jsonPath("$.name").isEqualTo("Name 1")
                .jsonPath("$.weight").isEqualTo(1)
                .jsonPath("$.serviceAddress").isNotEmpty();
    }

    @Test
    void duplicateError() {
        int productId = 1;
        postAndVerifyProduct(productId, OK);
        assertTrue(repository.findByProductId(productId).isPresent());
        postAndVerifyProduct(productId, UNPROCESSABLE_ENTITY)
                .jsonPath("$.path").isEqualTo("/product")
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: " + productId);
        assertEquals(1, repository.count());
    }

    @Test
    void deleteProduct() {
        int productId = 1;
        postAndVerifyProduct(productId, OK);
        assertTrue(repository.findByProductId(productId).isPresent());
        deleteAndVerifyProduct(productId, OK);
        assertFalse(repository.findByProductId(productId).isPresent());
        deleteAndVerifyProduct(productId, OK);
        getAndVerifyProduct(productId, NOT_FOUND);
    }

    @Test
    void invalidProductId() {
        getAndVerifyProduct(-1, UNPROCESSABLE_ENTITY)
                .jsonPath("$.message").isEqualTo("Invalid productId: -1");
    }

    private WebTestClient.BodyContentSpec postAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        Product product = new Product(productId, "Name " + productId, productId, "SA");
        return client.post().uri("/product").contentType(APPLICATION_JSON)
                .bodyValue(product).accept(APPLICATION_JSON).exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON).expectBody();
    }

    private WebTestClient.BodyContentSpec getAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        return client.get().uri("/product/" + productId).accept(APPLICATION_JSON).exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .expectHeader().contentType(APPLICATION_JSON).expectBody();
    }

    private void deleteAndVerifyProduct(int productId, HttpStatus expectedStatus) {
        client.delete().uri("/product/" + productId).exchange()
                .expectStatus().isEqualTo(expectedStatus).expectBody().isEmpty();
    }
}


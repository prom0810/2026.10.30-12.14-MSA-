package se.magnus.microservices.composite.product;

import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import se.magnus.microservices.composite.product.services.ProductCompositeIntegration;

import se.magnus.api.exceptions.NotFoundException;
import se.magnus.api.exceptions.InvalidInputException;


@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureWebTestClient
class ProductCompositeServiceApplicationTests {

	private static final int PRODUCT_ID_OK = 1;

	private static final int PRODUCT_ID_NOT_FOUND = 2;
	private static final int PRODUCT_ID_INVALID = 3;

	@Autowired
	private WebTestClient client;

	@MockitoBean
	private ProductCompositeIntegration compositeIntegration;

	@BeforeEach
	void setUp() {
		when(compositeIntegration.getProduct(PRODUCT_ID_OK))
				.thenReturn(new Product(
						PRODUCT_ID_OK,
						"name",
						1,
						"mock-address"));

		when(compositeIntegration.getRecommendations(PRODUCT_ID_OK))
				.thenReturn(List.of(new Recommendation(
						PRODUCT_ID_OK,
						1,
						"author",
						1,
						"content",
						"mock-address")));

		when(compositeIntegration.getReviews(PRODUCT_ID_OK))
				.thenReturn(List.of(new Review(
						PRODUCT_ID_OK,
						1,
						"author",
						"subject",
						"content",
						"mock-address")));

		when(compositeIntegration.getProduct(PRODUCT_ID_NOT_FOUND))
				.thenThrow(new NotFoundException(
						"NOT FOUND: " + PRODUCT_ID_NOT_FOUND));

		when(compositeIntegration.getProduct(PRODUCT_ID_INVALID))
				.thenThrow(new InvalidInputException(
						"INVALID: " + PRODUCT_ID_INVALID));
	}

	@Test
	void contextLoads() {
	}

	@Test
	void getProductById() {
		client.get()
				.uri("/product-composite/" + PRODUCT_ID_OK)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.productId").isEqualTo(PRODUCT_ID_OK)
				.jsonPath("$.recommendations.length()").isEqualTo(1)
				.jsonPath("$.reviews.length()").isEqualTo(1)
                .jsonPath("$.recommendations[0].content").isEqualTo("content")
                .jsonPath("$.reviews[0].content").isEqualTo("content");
	}

	@Test
	void getProductNotFound() {
		client.get()
				.uri("/product-composite/" + PRODUCT_ID_NOT_FOUND)
				.accept(APPLICATION_JSON)
				.exchange()
				.expectStatus().isNotFound()
				.expectHeader().contentType(APPLICATION_JSON)
				.expectBody()
				.jsonPath("$.path")
				.isEqualTo("/product-composite/" + PRODUCT_ID_NOT_FOUND)
				.jsonPath("$.message")
				.isEqualTo("NOT FOUND: " + PRODUCT_ID_NOT_FOUND);
	}

    @Test
    void createCompositeProduct() {
        client.post().uri("/product-composite").contentType(APPLICATION_JSON)
                .bodyValue("""
                    {"productId":10,"name":"New product","weight":5,
                     "recommendations":[{"recommendationId":2,"author":"A","rate":4,"content":"Recommendation text"}],
                     "reviews":[{"reviewId":3,"author":"B","subject":"Good","content":"Review text"}]}
                    """)
                .exchange().expectStatus().isOk();
        org.mockito.Mockito.verify(compositeIntegration).createProduct(org.mockito.ArgumentMatchers.argThat(
                p -> p.getProductId() == 10 && p.getName().equals("New product") && p.getWeight() == 5));
        org.mockito.Mockito.verify(compositeIntegration).createRecommendation(org.mockito.ArgumentMatchers.argThat(
                r -> r.getProductId() == 10 && r.getRecommendationId() == 2 && r.getRate() == 4 && "Recommendation text".equals(r.getContent())));
        org.mockito.Mockito.verify(compositeIntegration).createReview(org.mockito.ArgumentMatchers.argThat(
                r -> r.getProductId() == 10 && r.getReviewId() == 3 && r.getSubject().equals("Good") && "Review text".equals(r.getContent())));
    }

    @Test
    void deleteCompositeProductTwice() {
        for (int i = 0; i < 2; i++) {
            client.delete().uri("/product-composite/10").exchange().expectStatus().isOk();
        }
        org.mockito.Mockito.verify(compositeIntegration, org.mockito.Mockito.times(2)).deleteProduct(10);
        org.mockito.Mockito.verify(compositeIntegration, org.mockito.Mockito.times(2)).deleteRecommendations(10);
        org.mockito.Mockito.verify(compositeIntegration, org.mockito.Mockito.times(2)).deleteReviews(10);
    }
    @Test
    void createCompositeWithoutChildren() {
        client.post().uri("/product-composite").contentType(APPLICATION_JSON)
                .bodyValue(new se.magnus.api.composite.product.ProductAggregate(10, "name", 1, null, null, null))
                .exchange().expectStatus().isOk();
        org.mockito.Mockito.verify(compositeIntegration).createProduct(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(compositeIntegration, org.mockito.Mockito.never())
                .createRecommendation(org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.verify(compositeIntegration, org.mockito.Mockito.never())
                .createReview(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void duplicateProductErrorIsReturnedToClient() {
        when(compositeIntegration.createProduct(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new InvalidInputException("Duplicate key, Product Id: 10"));
        client.post().uri("/product-composite").contentType(APPLICATION_JSON)
                .bodyValue(new se.magnus.api.composite.product.ProductAggregate(10, "name", 1, null, null, null))
                .exchange().expectStatus().isEqualTo(422).expectBody()
                .jsonPath("$.message").isEqualTo("Duplicate key, Product Id: 10");
    }
}

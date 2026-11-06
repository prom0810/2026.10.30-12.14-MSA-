package se.magnus.microservices.composite.product;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.review.Review;
import tools.jackson.databind.json.JsonMapper;

class CoreResponseDeserializationTests {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void readsProductResponse() {
        Product product = mapper.readValue("""
            {"productId":123,"name":"name-123","weight":123,"serviceAddress":"product:8080"}
            """, Product.class);

        assertEquals(123, product.getProductId());
        assertEquals("name-123", product.getName());
        assertEquals(123, product.getWeight());
        assertEquals("product:8080", product.getServiceAddress());
    }

    @Test
    void readsRecommendationResponse() {
        Recommendation recommendation = mapper.readValue("""
            {"productId":123,"recommendationId":1,"author":"Author 1",
             "rate":5,"content":"Content 1","serviceAddress":"recommendation:8080"}
            """, Recommendation.class);

        assertEquals(123, recommendation.getProductId());
        assertEquals(1, recommendation.getRecommendationId());
        assertEquals("Author 1", recommendation.getAuthor());
        assertEquals(5, recommendation.getRate());
        assertEquals("Content 1", recommendation.getContent());
        assertEquals("recommendation:8080", recommendation.getServiceAddress());
    }

    @Test
    void readsReviewResponse() {
        Review review = mapper.readValue("""
            {"productId":123,"reviewId":2,"author":"Author 2",
             "subject":"Subject 2","content":"Content 2","serviceAddress":"review:8080"}
            """, Review.class);

        assertEquals(123, review.getProductId());
        assertEquals(2, review.getReviewId());
        assertEquals("Author 2", review.getAuthor());
        assertEquals("Subject 2", review.getSubject());
        assertEquals("Content 2", review.getContent());
        assertEquals("review:8080", review.getServiceAddress());
    }
}

package se.magnus.microservices.composite.product.services;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.springframework.http.HttpMethod.GET;

@Component
public class ProductCompositeIntegration implements ProductService, RecommendationService, ReviewService {
    private static final Logger LOG = LoggerFactory.getLogger(ProductCompositeIntegration.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    public ProductCompositeIntegration(RestTemplate restTemplate, ObjectMapper mapper,
            @Value("${app.product-service.host}") String productServiceHost,
            @Value("${app.product-service.port}") int productServicePort,
            @Value("${app.recommendation-service.host}") String recommendationServiceHost,
            @Value("${app.recommendation-service.port}") int recommendationServicePort,
            @Value("${app.review-service.host}") String reviewServiceHost,
            @Value("${app.review-service.port}") int reviewServicePort) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;
        productServiceUrl = "http://" + productServiceHost + ":" + productServicePort + "/product";
        recommendationServiceUrl = "http://" + recommendationServiceHost + ":" + recommendationServicePort + "/recommendation";
        reviewServiceUrl = "http://" + reviewServiceHost + ":" + reviewServicePort + "/review";
    }

    @Override
    public Product getProduct(int productId) {
        try {
            return restTemplate.getForObject(productServiceUrl + "/" + productId, Product.class);
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public Product createProduct(Product body) {
        return postEntity(productServiceUrl, body, Product.class);
    }

    @Override
    public void deleteProduct(int productId) {
        deleteEntity(productServiceUrl + "/" + productId);
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        return postEntity(recommendationServiceUrl, body, Recommendation.class);
    }

    @Override
    public Review createReview(Review body) {
        return postEntity(reviewServiceUrl, body, Review.class);
    }

    @Override
    public void deleteRecommendations(int productId) {
        deleteEntity(recommendationServiceUrl + "?productId=" + productId);
    }

    @Override
    public void deleteReviews(int productId) {
        deleteEntity(reviewServiceUrl + "?productId=" + productId);
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        try {
            return restTemplate.exchange(recommendationServiceUrl + "?productId=" + productId, GET, null,
                    new ParameterizedTypeReference<List<Recommendation>>() {}).getBody();
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    @Override
    public List<Review> getReviews(int productId) {
        try {
            return restTemplate.exchange(reviewServiceUrl + "?productId=" + productId, GET, null,
                    new ParameterizedTypeReference<List<Review>>() {}).getBody();
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    private <T> T postEntity(String url, T body, Class<T> responseType) {
        try {
            return restTemplate.postForObject(url, body, responseType);
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    private void deleteEntity(String url) {
        try {
            restTemplate.delete(url);
        } catch (HttpClientErrorException ex) {
            throw handleHttpClientException(ex);
        }
    }

    private RuntimeException handleHttpClientException(HttpClientErrorException ex) {
        return switch (ex.getStatusCode().value()) {
            case 404 -> new NotFoundException(getErrorMessage(ex));
            case 422 -> new InvalidInputException(getErrorMessage(ex));
            default -> {
                LOG.warn("Unexpected HTTP error: {}", ex.getStatusCode());
                yield ex;
            }
        };
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            JsonNode body = mapper.readTree(ex.getResponseBodyAsString());
            JsonNode message = body == null ? null : body.get("message");
            return message != null && message.isString() ? message.stringValue() : ex.getMessage();
        } catch (JacksonException parsingException) {
            return ex.getMessage();
        }
    }
}

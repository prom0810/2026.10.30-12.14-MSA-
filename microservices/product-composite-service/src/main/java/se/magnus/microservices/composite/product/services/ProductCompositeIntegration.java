package se.magnus.microservices.composite.product.services;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import se.magnus.api.core.product.Product;
import se.magnus.api.core.product.ProductService;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.http.HttpMethod.GET;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpClientErrorException;

import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.api.exceptions.NotFoundException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;

@Component
public class ProductCompositeIntegration
        implements ProductService, RecommendationService, ReviewService {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    private final String productServiceUrl;
    private final String recommendationServiceUrl;
    private final String reviewServiceUrl;

    private static final Logger LOG =
            LoggerFactory.getLogger(ProductCompositeIntegration.class);

    public ProductCompositeIntegration(
            RestTemplate restTemplate,
            ObjectMapper mapper,

            @Value("${app.product-service.host}")
            String productServiceHost,

            @Value("${app.product-service.port}")
            int productServicePort,

            @Value("${app.recommendation-service.host}")
            String recommendationServiceHost,

            @Value("${app.recommendation-service.port}")
            int recommendationServicePort,

            @Value("${app.review-service.host}")
            String reviewServiceHost,

            @Value("${app.review-service.port}")
            int reviewServicePort
    ) {
        this.restTemplate = restTemplate;
        this.mapper = mapper;

        this.productServiceUrl =
                "http://" + productServiceHost
                        + ":" + productServicePort + "/product/";

        this.recommendationServiceUrl =
                "http://" + recommendationServiceHost
                        + ":" + recommendationServicePort
                        + "/recommendation?productId=";

        this.reviewServiceUrl =
                "http://" + reviewServiceHost
                        + ":" + reviewServicePort
                        + "/review?productId=";
    }

    public Product getProduct(int productId) {
        try {
            String url = productServiceUrl + productId;

            LOG.debug("Will call getProduct API on URL: {}", url);

            return restTemplate.getForObject(url, Product.class);

        } catch (HttpClientErrorException ex) {
            switch (ex.getStatusCode().value()) {
                case 404:
                    throw new NotFoundException(getErrorMessage(ex));

                case 422:
                    throw new InvalidInputException(getErrorMessage(ex));

                default:
                    LOG.warn(
                            "Got an unexpected HTTP error: {}, will rethrow it",
                            ex.getStatusCode());
                    LOG.warn("Error body: {}", ex.getResponseBodyAsString());
                    throw ex;
            }
        }
    }

    private String getErrorMessage(HttpClientErrorException ex) {
        try {
            JsonNode body = mapper.readTree(ex.getResponseBodyAsString());
            JsonNode message = body.get("message");

            if (message != null && message.isString()) {
                return message.stringValue();
            }

            return ex.getMessage();

        } catch (JacksonException parsingException) {
            return ex.getMessage();
        }
    }

    public List<Recommendation> getRecommendations(int productId) {
        String url = recommendationServiceUrl + productId;
        List<Recommendation> recommendations =
                restTemplate.exchange(url, GET, null,
                                new ParameterizedTypeReference<List<Recommendation>>() {})
                        .getBody();
        return recommendations;
    }

    public List<Review> getReviews(int productId) {
        String url = reviewServiceUrl + productId;
        List<Review> reviews =
                restTemplate.exchange(url, GET, null,
                                new ParameterizedTypeReference<List<Review>>() {})
                        .getBody();
        return reviews;
    }
}
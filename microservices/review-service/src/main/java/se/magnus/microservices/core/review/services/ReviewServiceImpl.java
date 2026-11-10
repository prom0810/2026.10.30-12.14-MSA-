package se.magnus.microservices.core.review.services;

import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.review.Review;
import se.magnus.api.core.review.ReviewService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.review.persistence.ReviewEntity;
import se.magnus.microservices.core.review.persistence.ReviewRepository;
import se.magnus.util.http.ServiceUtil;

@RestController
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository repository;
    private final ServiceUtil serviceUtil;

    public ReviewServiceImpl(ReviewRepository repository, ServiceUtil serviceUtil) {
        this.repository = repository;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Review createReview(Review body) {
        try {
            ReviewEntity entity = repository.save(new ReviewEntity(body.getProductId(), body.getReviewId(),
                    body.getAuthor(), body.getSubject(), body.getContent()));
            return entityToApi(entity);
        } catch (DataIntegrityViolationException ex) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()
                    + ", Review Id: " + body.getReviewId());
        }
    }

    @Override
    public List<Review> getReviews(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        return repository.findByProductId(productId).stream().map(this::entityToApi).toList();
    }

    @Override
    public void deleteReviews(int productId) {
        repository.deleteAll(repository.findByProductId(productId));
    }

    private Review entityToApi(ReviewEntity entity) {
        return new Review(entity.getProductId(), entity.getReviewId(), entity.getAuthor(),
                entity.getSubject(), entity.getContent(), serviceUtil.getServiceAddress());
    }
}

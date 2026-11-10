package se.magnus.microservices.core.recommendation.services;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RestController;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.api.core.recommendation.RecommendationService;
import se.magnus.api.exceptions.InvalidInputException;
import se.magnus.microservices.core.recommendation.persistence.RecommendationRepository;
import se.magnus.util.http.ServiceUtil;

@RestController
public class RecommendationServiceImpl implements RecommendationService {
    private final RecommendationRepository repository;
    private final RecommendationMapper mapper;
    private final ServiceUtil serviceUtil;

    public RecommendationServiceImpl(RecommendationRepository repository, RecommendationMapper mapper,
                                     ServiceUtil serviceUtil) {
        this.repository = repository;
        this.mapper = mapper;
        this.serviceUtil = serviceUtil;
    }

    @Override
    public Recommendation createRecommendation(Recommendation body) {
        try {
            return mapper.entityToApi(repository.save(mapper.apiToEntity(body)));
        } catch (DuplicateKeyException ex) {
            throw new InvalidInputException("Duplicate key, Product Id: " + body.getProductId()
                    + ", Recommendation Id: " + body.getRecommendationId());
        }
    }

    @Override
    public List<Recommendation> getRecommendations(int productId) {
        if (productId < 1) throw new InvalidInputException("Invalid productId: " + productId);
        return repository.findByProductId(productId).stream().map(mapper::entityToApi)
                .map(r -> new Recommendation(r.getProductId(), r.getRecommendationId(), r.getAuthor(),
                        r.getRate(), r.getContent(), serviceUtil.getServiceAddress())).toList();
    }

    @Override
    public void deleteRecommendations(int productId) {
        repository.deleteAll(repository.findByProductId(productId));
    }
}

package se.magnus.microservices.core.recommendation.services;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import se.magnus.api.core.recommendation.Recommendation;
import se.magnus.microservices.core.recommendation.persistence.RecommendationEntity;

@Mapper(componentModel = "spring")
public interface RecommendationMapper {
    default Recommendation entityToApi(RecommendationEntity entity) {
        if (entity == null) return null;
        return new Recommendation(entity.getProductId(), entity.getRecommendationId(),
                entity.getAuthor(), entity.getRating(), entity.getContent(), null);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "rating", source = "api.rate")
    RecommendationEntity apiToEntity(Recommendation api);
}

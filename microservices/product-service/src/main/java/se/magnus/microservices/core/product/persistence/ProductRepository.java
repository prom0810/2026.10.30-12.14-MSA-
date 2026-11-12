package se.magnus.microservices.core.product.persistence;

import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ProductRepository extends ReactiveCrudRepository<ProductEntity, String> {
  Mono<ProductEntity> findByProductId(int productId);

  Flux<ProductEntity> findAllBy(Pageable pageable);
}

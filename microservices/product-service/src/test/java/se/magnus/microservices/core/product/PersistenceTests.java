package se.magnus.microservices.core.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static java.util.stream.IntStream.rangeClosed;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Mono;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.mongodb.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import reactor.test.StepVerifier;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.persistence.ProductRepository;

@DataMongoTest(properties = "spring.data.mongodb.auto-index-creation=true")
@Testcontainers
class PersistenceTests {

  @Container
  @ServiceConnection
  static MongoDBContainer mongo = new MongoDBContainer("mongo:8.0.5");

  @Autowired
  private ProductRepository repository;

  private ProductEntity savedEntity;

  @BeforeEach
  void setupDb() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();

    ProductEntity entity = new ProductEntity(1, "n", 1);
    StepVerifier.create(repository.save(entity))
      .expectNextMatches(createdEntity -> {
        savedEntity = createdEntity;
        return areProductEqual(entity, savedEntity);
      })
      .verifyComplete();
  }


  @Test
  void create() {
    ProductEntity newEntity = new ProductEntity(2, "n", 2);

    StepVerifier.create(repository.save(newEntity))
      .expectNextMatches(createdEntity -> newEntity.getProductId() == createdEntity.getProductId())
      .verifyComplete();

    StepVerifier.create(repository.findById(newEntity.getId()))
      .expectNextMatches(foundEntity -> areProductEqual(newEntity, foundEntity))
      .verifyComplete();

    StepVerifier.create(repository.count()).expectNext(2L).verifyComplete();
  }

  @Test
  void update() {
    savedEntity.setName("n2");
    StepVerifier.create(repository.save(savedEntity))
      .expectNextMatches(updatedEntity -> updatedEntity.getName().equals("n2"))
      .verifyComplete();

    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n2"))
      .verifyComplete();
  }

  @Test
  void delete() {
    StepVerifier.create(repository.delete(savedEntity)).verifyComplete();
    StepVerifier.create(repository.existsById(savedEntity.getId())).expectNext(false).verifyComplete();
  }

  @Test
  void getByProductId() {

    StepVerifier.create(repository.findByProductId(savedEntity.getProductId()))
      .expectNextMatches(foundEntity -> areProductEqual(savedEntity, foundEntity))
      .verifyComplete();
  }

  @Test
  void duplicateError() {
    ProductEntity entity = new ProductEntity(savedEntity.getProductId(), "n", 1);
    StepVerifier.create(repository.save(entity)).expectError(DuplicateKeyException.class).verify();
  }

  @Test
  void optimisticLockError() {

    // Store the saved entity in two separate entity objects
    ProductEntity entity1 = repository.findById(savedEntity.getId()).block();
    ProductEntity entity2 = repository.findById(savedEntity.getId()).block();

    // Update the entity using the first entity object
    entity1.setName("n1");
    repository.save(entity1).block();

    //  Update the entity using the second entity object.
    // This should fail since the second entity now holds a old version number, i.e. a Optimistic Lock Error
    StepVerifier.create(repository.save(entity2)).expectError(OptimisticLockingFailureException.class).verify();

    // Get the updated entity from the database and verify its new sate
    StepVerifier.create(repository.findById(savedEntity.getId()))
      .expectNextMatches(foundEntity ->
        foundEntity.getVersion() == 1
        && foundEntity.getName().equals("n1"))
      .verifyComplete();
  }

  @Test
  void paging() {
    StepVerifier.create(repository.deleteAll()).verifyComplete();

    var newProducts = rangeClosed(1001, 1010)
      .mapToObj(i -> new ProductEntity(i, "name " + i, i))
      .toList();
    StepVerifier.create(repository.saveAll(newProducts))
      .expectNextCount(10)
      .verifyComplete();

    Pageable nextPage =
      PageRequest.of(0, 4,
        Sort.Direction.ASC, "productId");
    nextPage = testNextPage(nextPage, "[1001, 1002, 1003, 1004]", true);
    nextPage = testNextPage(nextPage, "[1005, 1006, 1007, 1008]", true);
    testNextPage(nextPage, "[1009, 1010]", false);
  }

  private Pageable testNextPage(
      Pageable nextPage,
      String expectedProductIds, boolean expectsNextPage) {
    StepVerifier.create(Mono.zip(
        repository.findAllBy(nextPage).collectList(), repository.count()))
      .assertNext(results -> {
        var page = new PageImpl<>(
          results.getT1(), nextPage, results.getT2());
        assertEquals(expectedProductIds,
          page.getContent().stream().map(ProductEntity::getProductId).toList().toString());
        assertEquals(expectsNextPage, page.hasNext());
      })
      .verifyComplete();
    return nextPage.next();
  }
  private boolean areProductEqual(ProductEntity expectedEntity, ProductEntity actualEntity) {
    return
      (expectedEntity.getId().equals(actualEntity.getId()))
      && (expectedEntity.getVersion() == actualEntity.getVersion())
      && (expectedEntity.getProductId() == actualEntity.getProductId())
      && (expectedEntity.getName().equals(actualEntity.getName()))
      && (expectedEntity.getWeight() == actualEntity.getWeight());
  }
}

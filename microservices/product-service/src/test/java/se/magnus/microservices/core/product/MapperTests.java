package se.magnus.microservices.core.product;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import se.magnus.api.core.product.Product;
import se.magnus.microservices.core.product.persistence.ProductEntity;
import se.magnus.microservices.core.product.services.ProductMapper;
import static org.junit.jupiter.api.Assertions.*;

class MapperTests {
    private final ProductMapper mapper = Mappers.getMapper(ProductMapper.class);

    @Test
    void mapsProductFieldsAndIgnoresPersistenceMetadata() {
        ProductEntity entity = new ProductEntity(7, "Product", 12);
        entity.setId("database-id");
        entity.setVersion(3);
        Product api = mapper.entityToApi(entity);
        assertEquals(7, api.getProductId());
        assertEquals("Product", api.getName());
        assertEquals(12, api.getWeight());
        assertNull(api.getServiceAddress());
        ProductEntity mapped = mapper.apiToEntity(api);
        assertEquals(7, mapped.getProductId());
        assertEquals("Product", mapped.getName());
        assertEquals(12, mapped.getWeight());
        assertNull(mapped.getId());
        assertNull(mapped.getVersion());
    }
}

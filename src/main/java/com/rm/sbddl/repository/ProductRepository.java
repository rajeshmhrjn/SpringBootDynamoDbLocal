package com.rm.sbddl.repository;


import com.rm.sbddl.model.Product;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ProductRepository {

    private final DynamoDbTable<Product> table;

    public ProductRepository(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("Products", TableSchema.fromBean(Product.class));
    }

    public Product save(Product product) {
        if (product.getId() == null || product.getId().isBlank()) {
            product.setId(UUID.randomUUID().toString());
        }
        table.putItem(product);
        return product;
    }

    public Optional<Product> findById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        Product product = table.getItem(key);
        return Optional.ofNullable(product);
    }

    public List<Product> findAll() {
        //This is just for learning. Dont use scan() in real environments
        PageIterable<Product> pages = table.scan();
        return pages.items().stream().collect(Collectors.toList());
    }

    public Optional<Product> update(String id, Product updated) {
        return findById(id).map(existing -> {
            updated.setId(id);
            table.putItem(updated);
            return updated;
        });
    }

    public boolean deleteById(String id) {
        Key key = Key.builder().partitionValue(id).build();
        Product deleted = table.deleteItem(key);
        return deleted != null;
    }
}

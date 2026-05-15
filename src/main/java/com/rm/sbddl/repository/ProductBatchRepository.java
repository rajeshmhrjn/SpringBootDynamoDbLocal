package com.rm.sbddl.repository;


import com.rm.sbddl.model.BatchResult;
import com.rm.sbddl.model.Product;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public class ProductBatchRepository {

    private final DynamoDbEnhancedClient enhancedClient;
    private final DynamoDbTable<Product> table;

    private static final int DYNAMODB_BATCH_LIMIT = 25;

    public ProductBatchRepository(DynamoDbEnhancedClient enhancedClient) {
        this.enhancedClient = enhancedClient;
        this.table = enhancedClient.table("Products", TableSchema.fromBean(Product.class));
    }

    // ── Batch Insert ────────────────────────────────────────────────────────

    public List<Product> batchSave(List<Product> products) {
        //DynamoDb can only send 25 items to write in batch. If there are 26 items, you'll have to make 2 calls
        List<List<Product>> productListPartitions = partition(products, DYNAMODB_BATCH_LIMIT);
        productListPartitions.forEach(chunk -> {
            WriteBatch.Builder<Product> batchBuilder = WriteBatch.builder(Product.class).mappedTableResource(table);

            chunk.forEach(product -> {
                if (product.getId() == null || product.getId().isBlank()) {
                    product.setId(UUID.randomUUID().toString());
                }
                batchBuilder.addPutItem(r -> r.item(product));
            });

            enhancedClient.batchWriteItem(r -> r.writeBatches(batchBuilder.build()));
        });

        return products;
    }

    // ── Batch Update ────────────────────────────────────────────────────────

    public BatchResult<Product> batchUpdate(List<Product> products) {
        List<Product> found = new ArrayList<>();
        List<Product> notFound = new ArrayList<>();

        // Fetch all IDs in a single DynamoDB request
        List<String> ids = products.stream()
                .map(Product::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());

        Set<String> existingIds = batchGetByIds(ids).stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toSet());

        products.forEach(p -> {
            if (p.getId() != null && existingIds.contains(p.getId())) {
                found.add(p);
            } else {
                notFound.add(p);
            }
        });

        if (!found.isEmpty()) {
            partition(found, DYNAMODB_BATCH_LIMIT).forEach(chunk -> {
                WriteBatch.Builder<Product> batchBuilder = WriteBatch.builder(Product.class).mappedTableResource(table);

                chunk.forEach(p -> batchBuilder.addPutItem(r -> r.item(p)));
                enhancedClient.batchWriteItem(r -> r.writeBatches(batchBuilder.build()));
            });
        }

        return new BatchResult<>(found, notFound);
    }

    // ── Batch Delete ────────────────────────────────────────────────────────

    public BatchResult<String> batchDelete(List<String> ids) {
        // Fetch all IDs in a single DynamoDB request
        List<String> found = new ArrayList<>();
        List<String> notFound = new ArrayList<>(ids);

        // Build batch get keys
        List<Product> fetchedProducts = batchGetByIds(ids);
        Set<String> existingIds = fetchedProducts.stream()
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toSet());

        ids.forEach(id -> {
            if (existingIds.contains(id)) {
                found.add(id);
            }
        });
        notFound.removeAll(existingIds);

        // Delete the ones that exist
        if (!found.isEmpty()) {
            partition(found, DYNAMODB_BATCH_LIMIT).forEach(chunk -> {
                WriteBatch.Builder<Product> batchBuilder = WriteBatch.builder(Product.class)
                        .mappedTableResource(table);

                chunk.forEach(id -> batchBuilder.addDeleteItem(
                        r -> r.key(Key.builder().partitionValue(id).build())
                ));
                enhancedClient.batchWriteItem(r -> r.writeBatches(batchBuilder.build()));
            });
        }

        return new BatchResult<>(found, notFound);
    }

    // ── Helper: chunk list into pages of `size` ─────────────────────────────

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return partitions;
    }

    // ── Inner result wrapper ────────────────────────────────────────────────

    // ── Helper: fetch multiple items in one request ─────────────────────────
    public List<Product> batchGetByIds(List<String> ids) {
        List<Product> results = new ArrayList<>();

        //DynamoDb can only read 100 items at a time. If you have 101 items, then you'll need to make 2 network call
        List<List<String>> productIdListPartition = partition(ids, 100);

        productIdListPartition.forEach(chunk -> {
            // addGetItem only accepts one key at a time — loop to add each
            ReadBatch.Builder<Product> readBatchBuilder = ReadBatch.builder(Product.class).mappedTableResource(table);

            chunk.forEach(id -> readBatchBuilder.addGetItem(
                    GetItemEnhancedRequest.builder()
                            .key(Key.builder().partitionValue(id).build())
                            .build()
            ));

            BatchGetResultPageIterable resultPages = enhancedClient.batchGetItem(
                    r -> r.readBatches(readBatchBuilder.build())
            );

            resultPages.resultsForTable(table).forEach(results::add);
        });

        return results;
    }


}
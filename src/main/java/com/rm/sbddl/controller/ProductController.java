package com.rm.sbddl.controller;


import com.rm.sbddl.dto.BatchGetRequest;
import com.rm.sbddl.dto.BatchGetResponse;
import com.rm.sbddl.dto.BatchResult;
import com.rm.sbddl.model.Product;
import com.rm.sbddl.repository.ProductBatchRepository;
import com.rm.sbddl.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductRepository repository;
    private final ProductBatchRepository batchRepository;

    public ProductController(ProductRepository repository, ProductBatchRepository batchRepository) {
        this.repository = repository;
        this.batchRepository = batchRepository;
    }

    // CREATE
    @PostMapping
    public ResponseEntity<Product> create(@RequestBody Product product) {
        Product saved = repository.save(product);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // READ ALL
    @GetMapping
    public ResponseEntity<List<Product>> getAll() {
        return ResponseEntity.ok(repository.findAll());
    }

    // READ ONE
    @GetMapping("/{id}")
    public ResponseEntity<Product> getById(@PathVariable String id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // UPDATE
    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable String id,
                                          @RequestBody Product product) {
        return repository.update(id, product)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // DELETE
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean deleted = repository.deleteById(id);
        return deleted
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }


    // ── Batch operations ────────────────────────────────────────────────────

    @PostMapping("/batch")
    public ResponseEntity<List<Product>> batchCreate(@RequestBody List<Product> products) {
        List<Product> cretedProductList = batchRepository.batchSave(products);
        return ResponseEntity.status(HttpStatus.CREATED).body(cretedProductList);
    }


    @PostMapping("/batch-get")
    public ResponseEntity<BatchGetResponse> batchGet(@RequestBody BatchGetRequest request) {
        List<Product> found = batchRepository.batchGetByIds(request.ids());
        return ResponseEntity.ok(BatchGetResponse.of(found, request.ids()));
    }

    @PutMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchUpdate(@RequestBody List<Product> products) {
        BatchResult<Product> result = batchRepository.batchUpdate(products);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("updated", result.succeeded());
        response.put("notFound", result.failed());
        response.put("updatedCount", result.succeeded().size());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestBody List<String> ids) {
        BatchResult<String> result = batchRepository.batchDelete(ids);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("deleted", result.succeeded());
        response.put("notFound", result.failed());
        response.put("deletedCount", result.succeeded().size());

        return ResponseEntity.ok(response);
    }
}

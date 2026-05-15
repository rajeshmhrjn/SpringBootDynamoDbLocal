package com.rm.sbddl.model;

import java.util.List;

public record BatchGetResponse(
        List<Product> found,
        List<String> notFound,
        int foundCount,
        int notFoundCount
) {
    public static BatchGetResponse of(List<Product> found, List<String> requestedIds) {
        List<String> foundIds = found.stream()
                .map(Product::getId)
                .toList();

        List<String> notFound = requestedIds.stream()
                .filter(id -> !foundIds.contains(id))
                .toList();

        return new BatchGetResponse(found, notFound, found.size(), notFound.size());
    }
}

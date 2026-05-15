package com.rm.sbddl.model;

import java.util.List;

public record BatchGetRequest(List<String> ids) {

    public BatchGetRequest {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ids must not be null or empty");
        }
    }
}
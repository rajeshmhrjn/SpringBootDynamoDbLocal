package com.rm.sbddl.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@DynamoDbBean
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    private String id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { return id; }

    @DynamoDbAttribute("name")
    public String getName() { return name; }

    @DynamoDbAttribute("description")
    public String getDescription() { return description; }

    @DynamoDbAttribute("price")
    public Double getPrice() { return price; }

    @DynamoDbAttribute("stock")
    public Integer getStock() { return stock; }
}

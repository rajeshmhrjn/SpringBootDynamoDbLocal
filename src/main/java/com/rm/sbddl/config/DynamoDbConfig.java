package com.rm.sbddl.config;

import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;

@Configuration
public class DynamoDbConfig {

    private static final String PORT = "8000";
    private static final String TABLE_NAME = "Products";

    private DynamoDBProxyServer server;

    // ✅ Start the server and return the client — no @PostConstruct involved
    @Bean
    public DynamoDbClient dynamoDbClient() throws Exception {
        startEmbeddedDynamoDB();

        DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://localhost:" + PORT))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("fakeKey", "fakeSecret")
                ))
                .build();

        createTableIfNotExists(client);
        return client;
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }

    private void startEmbeddedDynamoDB() throws Exception {
        System.setProperty("sqlite4java.library.path", "target/dynamodb-local-libs");

        server = ServerRunner.createServerFromCommandLineArgs(
                new String[]{"-inMemory", "-port", PORT}
        );
        server.start();
        System.out.println("✅ DynamoDB Local started on port " + PORT);
    }

    private void createTableIfNotExists(DynamoDbClient client) {
        try {
            boolean exists = client.listTables().tableNames().contains(TABLE_NAME);
            if (exists) {
                System.out.println("Table '" + TABLE_NAME + "' already exists.");
                return;
            }

            client.createTable(CreateTableRequest.builder()
                    .tableName(TABLE_NAME)
                    .keySchema(
                            KeySchemaElement.builder()
                                    .attributeName("id")
                                    .keyType(KeyType.HASH)
                                    .build()
                    )
                    .attributeDefinitions(
                            AttributeDefinition.builder()
                                    .attributeName("id")
                                    .attributeType(ScalarAttributeType.S)
                                    .build()
                    )
                    .billingMode(BillingMode.PAY_PER_REQUEST)
                    .build());

            System.out.println("Table '" + TABLE_NAME + "' created.");

        } catch (Exception e) {
            System.err.println("X Error creating table: " + e.getMessage());
            throw new RuntimeException("Failed to create DynamoDB table", e);
        }
    }

    @PreDestroy
    public void stopEmbeddedDynamoDB() throws Exception {
        if (server != null) {
            server.stop();
            System.out.println("DynamoDB Local stopped.");
        }
    }
}
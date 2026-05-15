# Spring Boot + DynamoDB Local — Learning Project

A hands-on practice project that integrates **Spring Boot 3.5** with **DynamoDB Local** (embedded, in-memory).  
No AWS account required — the database starts inside the JVM alongside the application.

---

## What This Project Covers

| Topic | Details |
|---|---|
| Embedded DynamoDB | DynamoDB Local 2.5.2 started programmatically via `ServerRunner` |
| AWS SDK v2 | Low-level `DynamoDbClient` + high-level `DynamoDbEnhancedClient` |
| Table lifecycle | Auto-create table on startup, auto-stop server on shutdown (`@PreDestroy`) |
| CRUD REST API | Full create / read / update / delete over HTTP |
| Batch operations | Batch write (25-item limit), batch read (100-item limit), batch update, batch delete |
| Chunking/partitioning | Helper that splits large lists to respect DynamoDB's per-request limits |
| Lombok | `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` reduce boilerplate |

---

## Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 21 |
| Framework | Spring Boot | 3.5.0 |
| AWS SDK | DynamoDB (v2) + Enhanced Client | 2.25.40 |
| Local DB | DynamoDB Local | 2.5.2 |
| HTTP runtime | Jetty (ee10) | managed by Spring Boot |
| Build | Maven | wrapper included |
| Utilities | Lombok | managed by Spring Boot |

---

## Project Structure

```
src/main/java/com/rm/sbddl/
├── SpringBootDynamoDbLocalApplication.java   # Entry point
├── config/
│   └── DynamoDbConfig.java                   # Starts DynamoDB Local, creates the table,
│                                             #   wires DynamoDbClient + DynamoDbEnhancedClient
├── controller/
│   └── ProductController.java                # REST endpoints for CRUD + batch operations
├── model/
│   ├── Product.java                          # DynamoDB-mapped entity (@DynamoDbBean)
│   ├── BatchGetRequest.java                  # Request record for batch-get endpoint
│   ├── BatchGetResponse.java                 # Response record with found/missing breakdown
│   └── BatchResult.java                      # Generic wrapper: succeeded + failed lists
└── repository/
    ├── ProductRepository.java                # Single-item CRUD via DynamoDbEnhancedClient
    └── ProductBatchRepository.java           # Batch CRUD with automatic chunking
```

---

## Key Concepts Learned

### 1. Embedded DynamoDB Local
`DynamoDbConfig` starts the local server programmatically before creating the AWS client:

```java
// target/dynamodb-local-libs must contain the DynamoDBLocal JAR (copied by maven-dependency-plugin)
System.setProperty("sqlite4java.library.path", "target/dynamodb-local-libs");
server = ServerRunner.createServerFromCommandLineArgs(new String[]{"-inMemory", "-port", "8000"});
server.start();
```

The `-inMemory` flag means **all data is lost when the app stops** — perfect for local dev/testing.

### 2. Fake AWS Credentials for Local Development
DynamoDB Local accepts any non-empty credential values:

```java
.credentialsProvider(StaticCredentialsProvider.create(
    AwsBasicCredentials.create("fakeKey", "fakeSecret")
))
```

### 3. DynamoDB Enhanced Client
`DynamoDbEnhancedClient` maps a Java class to a DynamoDB table using annotations on getter methods:

```java
@DynamoDbBean
public class Product {
    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() { ... }
}
```

The table schema is derived at runtime via `TableSchema.fromBean(Product.class)`.

### 4. DynamoDB Batch Limits
DynamoDB enforces hard limits per request that you must handle manually:

| Operation | Max items per request |
|---|---|
| `batchWriteItem` (put/delete) | **25** |
| `batchGetItem` | **100** |

The `partition()` helper in `ProductBatchRepository` splits any list into chunks before sending:

```java
// Splits [0..N] into sub-lists of at most `size` elements
private <T> List<List<T>> partition(List<T> list, int size) { ... }
```

### 5. `scan()` — Use Only for Learning
`ProductRepository.findAll()` uses `table.scan()`, which reads **every item** in the table.  
This is acceptable here for simplicity, but in production it is expensive and should be avoided.

---

## Running the Application

### Prerequisites
- Java 21+
- Maven (or use the included `./mvnw` wrapper)

### Start

```bash
./mvnw spring-boot:run
```

The application:
1. Copies the DynamoDB Local JAR to `target/dynamodb-local-libs/`
2. Starts DynamoDB Local in-memory on **port 8000**
3. Creates the `Products` table automatically
4. Starts the Spring Boot server on **port 8080**

---

## REST API Reference

Base URL: `http://localhost:8080/api/products`

### Single-item Operations

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/products` | Create a product (auto-generates `id` if not provided) |
| `GET` | `/api/products` | List all products (uses `scan()` — learning only) |
| `GET` | `/api/products/{id}` | Get a product by ID |
| `PUT` | `/api/products/{id}` | Update a product by ID |
| `DELETE` | `/api/products/{id}` | Delete a product by ID |

### Batch Operations

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/products/batch` | Create multiple products (chunked at 25) |
| `POST` | `/api/products/batch-get` | Fetch multiple products by IDs (chunked at 100) |
| `PUT` | `/api/products/batch` | Update multiple products; returns `updated` + `notFound` lists |
| `DELETE` | `/api/products/batch` | Delete multiple products by ID; returns `deleted` + `notFound` lists |

### Example Payloads

**Create a product**
```json
POST /api/products
{
  "name": "Laptop",
  "description": "15-inch laptop",
  "price": 999.99,
  "stock": 10
}
```

**Batch create**
```json
POST /api/products/batch
[
  { "name": "Mouse", "price": 29.99, "stock": 50 },
  { "name": "Keyboard", "price": 49.99, "stock": 30 }
]
```

**Batch get**
```json
POST /api/products/batch-get
{
  "ids": ["id-1", "id-2", "id-3"]
}
```

**Batch delete**
```json
DELETE /api/products/batch
["id-1", "id-2"]
```

---

## Configuration

`src/main/resources/application.properties`:

```properties
spring.application.name=SpringBootDynamoDbLocal
server.port=8080
logging.level.com.rm.sbddl=DEBUG
```

DynamoDB Local port (`8000`) is configured as a constant in `DynamoDbConfig.java`.

---

## How DynamoDB Local Is Resolved

The `pom.xml` adds the Amazon DynamoDB Local Maven repository:

```xml
<repository>
    <id>dynamodb-local</id>
    <url>https://s3.us-west-2.amazonaws.com/dynamodb-local/release</url>
</repository>
```

The `maven-dependency-plugin` copies the JAR to `target/dynamodb-local-libs/` during the build,
which is where the SQLite native library loader looks at startup.

---

## Notes / Gotchas

- **Data is not persisted** — the `-inMemory` flag means the database resets every time the app restarts.
- **No real AWS credentials needed** — any non-empty key/secret works with DynamoDB Local.
- **Jetty conflict** — DynamoDB Local embeds Jetty internally. The `pom.xml` explicitly includes `jetty-ee10-servlet` and re-adds `spring-boot-starter-tomcat` to prevent classpath conflicts.
- **Circular dependency avoided** — the embedded server is started inside the `@Bean` method rather than `@PostConstruct` to keep Spring's initialization order predictable.

---

## References

- [DynamoDB Local documentation](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBLocal.html)
- [AWS SDK for Java v2 — DynamoDB Enhanced Client](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/dynamodb-enhanced-client.html)
- [Spring Boot 3.x reference](https://docs.spring.io/spring-boot/docs/3.5.x/reference/html/)

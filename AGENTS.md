# AGENTS.md - Development Guidelines

## Project Overview
- **Project**: Spring AI with local LLAMA3 (experiment-ai-llm)
- **Type**: Spring Boot Web Application with AI/RAG capabilities
- **Java Version**: 21
- **Build Tool**: Maven

## Build & Test Commands

### Build
```bash
mvn clean install          # Full build with tests
mvn clean package         # Package only (skip tests)
mvn spring-boot:run       # Run application locally
```

### Running Tests
```bash
mvn test                           # Run all tests
mvn test -Dtest=TestName          # Run single test class
mvn test -Dtest=TestName#method   # Run single test method
mvn verify                        # Run tests and verify
```

### Linting & Code Quality
```bash
# Qodana (static analysis)
docker run --rm -v $(pwd):/project jetbrains/qodana-jvm-community:2025.3

# Maven wrapper available
./mvnw clean install
./mvnw test
```

### Database
```bash
# Start PostgreSQL with pgvector
docker-compose up -d

# Or manually:
docker run -d -e POSTGRES_USER=dev -e POSTGRES_PASSWORD=pass -e POSTGRES_DB=ai_db -p 5432:5432 ankane/pgvector:latest
```

## Code Style Guidelines

### General Conventions
- **Language**: German comments preferred (project is German-speaking)
- **Architecture**: MVC pattern with Controllers, Services, Repositories, DAO/Models
- **Package structure**: `it.gdorsi.{controller,service,dao,repository,config,exception}`

### Naming Conventions
- **Classes**: PascalCase (e.g., `ChatController`, `VertragService`)
- **Methods**: camelCase (e.g., `saveContractFromPdf`)
- **Records**: PascalCase (e.g., `VertragRequest`)
- **Constants**: UPPER_SNAKE_CASE
- **Packages**: lowercase, singular

### Java 21 Features
- Use **records** for immutable DTOs and requests
- Use **pattern matching** where appropriate
- Enable compiler parameters: `<maven.compiler.parameters>true</maven.compiler.parameters>`

### Lombok Usage
Lombok is used throughout the project:
```java
@Slf4j              // Logging
@Component          // Spring bean
@RequiredArgsConstructor  // Constructor injection
@Data               // Use sparingly (only when mutability needed)
```

### Record Usage
Prefer records for DTOs:
```java
public record VertragRequest(
    String vertragsNummer,
    String kundeName,
    LocalDate startDatum,
    // ...
) {}
```

### Dependency Injection
Use **constructor injection** via `@RequiredArgsConstructor` (Lombok):
```java
@Component
@RequiredArgsConstructor
public class VertragTool {
    private final VertragRepository repository;
    private final EmbeddingModel embeddingModel;
}
```

### Import Organization
Standard order (no blank lines between groups):
1. `java.*`
2. `javax.*`
3. `org.*` (Spring, external)
4. `it.gdorsi.*` (project)

### Error Handling
- Use `@Transactional` for database operations
- Log errors with `@Slf4j`: `log.error("message: {}", detail, exception)`
- Return meaningful error messages (German)
- Use try-catch in service layer for recovery

### Database
- **ORM**: JPA/Hibernate with Spring Data
- **Migrations**: Liquibase (changelog in `src/main/resources/db/changelog/`)
- **Vector Store**: pgvector for embeddings
- Use **entities** in `repository.model` package

### REST Controllers
- Use `@Controller` for Thymeleaf views
- Use `@RestController` for pure JSON APIs
- Add `@Valid` for request validation
- Use proper HTTP status codes

### Testing
- Test classes in `src/test/java/`
- Use `@SpringBootTest` for integration tests
- Configuration in `src/test/resources/` or `src/test/application.properties`

### Configuration
- Main config: `src/main/resources/application.properties`
- Docker: `src/main/resources/docker-compose.yml`
- Database changelogs: `src/main/resources/db/changelog/`

### AI/ML Integration
- Spring AI with Ollama for local LLAMA3
- EmbeddingModel for vector generation
- VectorStore (pgvector) for semantic search
- ChatClient for LLM interactions
- Use RAG pattern for document Q&A

### IDE Setup
- Project uses Eclipse/Maven structure
- Lombok requires annotation processing enabled
- Qodana analysis configured in `qodana.yaml`
- GitHub Actions CI in `.github/workflows/`

## CI/CD
- GitHub Actions workflow: `.github/workflows/maven.yml`
- Qodana quality check: `.github/workflows/qodana_code_quality.yml`
- Runs on push/PR to main branch
- Java 21 with Corretto distribution

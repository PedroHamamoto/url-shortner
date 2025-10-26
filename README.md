# Shortifier

A modern URL shortening microservice built with Spring Boot 3.5.6 and Java 25. Shortifier provides a RESTful API to create shortened URLs, manage redirects, and track access with optional expiration support.

## Overview

Shortifier is a production-ready URL shortening service that converts long URLs into 5-character short codes. The service includes a responsive web UI for easy URL shortening, comprehensive error handling, collision detection, and optional URL expiration. Built with Spring Boot, PostgreSQL, and deployed with Docker.

**Current Status**: Early development stage with core functionality implemented and fully tested. REST API endpoints operational, database schema in place, UI fully functional.

## Features

- **URL Shortening**: Convert long URLs to 5-character Base62 encoded short codes
- **Collision Detection**: Automatic retry mechanism with unique code generation (max 10 retries)
- **URL Expiration**: Optional expiration dates for shortened URLs with automatic validation
- **Redirect Tracking**: Access count tracking for each shortened URL
- **Comprehensive Error Handling**: Global exception handler with proper HTTP status codes (400, 404, 410, 500)
- **Input Validation**: Server-side URL validation with clear error messages
- **CORS Support**: Cross-origin resource sharing enabled for API consumption
- **Responsive Web UI**: Mobile-friendly interface with Tailwind CSS for easy URL shortening
- **Client-side History**: Browser-based URL history with localStorage persistence
- **Health Monitoring**: Spring Boot Actuator endpoints for application health and metrics
- **Database Migrations**: Liquibase-managed schema versioning with PostgreSQL
- **Comprehensive Testing**: Unit tests, integration tests, and smoke tests with JUnit 5

## Technology Stack

- **Java 25** - Latest JDK with advanced language features
- **Spring Boot 3.5.6** - Framework for building microservices
  - Spring Web - REST API support
  - Spring Data JPA - ORM with Hibernate
  - Spring Boot Actuator - Monitoring and management
  - Spring Boot DevTools - Development-time features
- **PostgreSQL** - Production database
- **H2** - In-memory database for testing
- **Liquibase** - Database schema versioning
- **Lombok** - Reduces boilerplate code
- **Gradle** - Build automation with Kotlin DSL
- **Docker** - Containerized PostgreSQL for local development
- **JUnit 5** - Testing framework

## Prerequisites

- Java 25 or higher
- Docker and Docker Compose (for local PostgreSQL)
- Gradle 8.x (wrapper included)

## Getting Started

### 1. Start PostgreSQL Database

```bash
docker-compose up -d
```

This starts a PostgreSQL container with:
- Database: `shortifier`
- User: `shortifier_user`
- Password: `shortifier_pass`
- Port: `5432`

### 2. Build the Project

```bash
./gradlew build
```

### 3. Run the Application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### 4. Verify Health Status

```bash
curl http://localhost:8080/actuator/health
```

## Available Commands

### Building and Running

```bash
# Build the project
./gradlew build

# Run the application
./gradlew bootRun

# Build executable JAR
./gradlew bootJar

# Clean build artifacts
./gradlew clean

# Assemble without tests
./gradlew assemble
```

### Testing

```bash
# Run all tests
./gradlew test

# Run with test runtime classpath
./gradlew bootTestRun

# Run a specific test class
./gradlew test --tests com.hamamoto.shortifier.ClassName

# Run a specific test method
./gradlew test --tests com.hamamoto.shortifier.ClassName.methodName
```

### Database Management

Database migrations are managed by Liquibase and run automatically on application startup.

Migration files are located in: `src/main/resources/db/changelog/`

### Docker Commands

```bash
# Start PostgreSQL
docker-compose up -d

# Stop PostgreSQL
docker-compose down

# View logs
docker-compose logs -f

# Remove data volume (fresh start)
docker-compose down -v
```

## API Endpoints

### Implemented Endpoints

#### 1. Create Shortened URL
```http
POST /api/shorten
Content-Type: application/json

{
  "url": "https://example.com/very/long/url/path",
  "expiresAt": "2025-12-31T23:59:59"  (optional)
}
```

**Response (201 Created)**:
```json
{
  "shortCode": "aBc12",
  "originalUrl": "https://example.com/very/long/url/path",
  "shortUrl": "http://localhost:8080/aBc12",
  "createdAt": "2025-10-26T10:30:00",
  "expiresAt": "2025-12-31T23:59:59"
}
```

**Error Responses**:
- `400 Bad Request`: Invalid URL format, missing URL, or URL too long
- `500 Internal Server Error`: Unable to generate unique short code after retries

#### 2. Redirect to Original URL
```http
GET /{shortCode}
```

**Response (302 Found)**:
- Redirects to the original URL if found and not expired
- HTTP Status: `302 Found` with `Location` header set to original URL

**Error Responses**:
- `404 Not Found`: Short code does not exist
- `410 Gone`: Short code exists but URL has expired

#### 3. Health Check
```http
GET /actuator/health
```

**Response (200 OK)**:
```json
{
  "status": "UP"
}
```

### Request/Response Examples

**Example 1: Successful URL Shortening**
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://github.com/hamamoto/shortifier"}'
```

**Example 2: URL Shortening with Expiration**
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/test", "expiresAt": "2025-12-31T23:59:59"}'
```

**Example 3: Redirect from Short Code**
```bash
curl -L -v http://localhost:8080/aBc12
```

**Example 4: Accessing Expired URL**
```bash
curl -v http://localhost:8080/ExpiredCode
# Returns: 410 Gone with error response
```

## Project Architecture

### Directory Structure

```
shortifier/
├── build.gradle.kts                          # Gradle build configuration
├── docker-compose.yml                        # PostgreSQL container setup
├── settings.gradle.kts                       # Gradle project settings
├── README.md                                 # This file
├── CLAUDE.md                                 # Development guide
│
├── src/main/java/com/hamamoto/shortifier/
│   ├── ShortifierApplication.java            # Spring Boot entry point
│   ├── config/
│   │   └── WebConfig.java                    # CORS configuration
│   ├── controller/
│   │   └── UrlShortenerController.java       # REST API endpoints
│   ├── service/
│   │   └── UrlShortenerService.java          # Business logic
│   ├── entity/
│   │   └── UrlMapping.java                   # JPA entity
│   ├── repository/
│   │   └── UrlMappingRepository.java         # Spring Data JPA repository
│   ├── dto/
│   │   ├── ShortenRequest.java               # Request DTO
│   │   ├── ShortenResponse.java              # Response DTO
│   │   └── ErrorResponse.java                # Error response DTO
│   └── exception/
│       ├── GlobalExceptionHandler.java       # Exception handler
│       ├── ShortUrlNotFoundException.java    # 404 exception
│       └── ShortUrlExpiredException.java     # 410 exception
│
├── src/main/resources/
│   ├── application.properties                # Main configuration
│   └── db/changelog/
│       ├── db.changelog-master.yaml          # Liquibase master
│       └── changes/
│           └── 001-create-url_mapping-table.yaml
│
├── src/test/java/com/hamamoto/shortifier/
│   ├── ShortifierApplicationTests.java       # Smoke test
│   ├── service/
│   │   └── UrlShortenerServiceTest.java      # Service unit tests
│   └── controller/
│       └── UrlShortenerControllerIntegrationTest.java
│
├── src/test/resources/
│   └── application.properties                # Test configuration
│
└── ui/
    ├── index.html                            # Web UI
    └── js/
        └── app.js                            # Client-side logic
```

### Component Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Web Browser UI                       │
│  (HTML + Tailwind CSS + Vanilla JavaScript)             │
│  - URL input form                                       │
│  - History management                                   │
│  - Toast notifications                                  │
└────────────────┬────────────────────────────────────────┘
                 │ HTTP
┌────────────────▼────────────────────────────────────────┐
│           Spring Boot REST API                          │
│  UrlShortenerController (UrlShortenerController.java)   │
│  ├─ POST /api/shorten                                   │
│  └─ GET /{shortCode}                                    │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────┐
│          Business Logic Layer                           │
│  UrlShortenerService (UrlShortenerService.java)         │
│  ├─ Short code generation (Base62)                      │
│  ├─ Collision detection & retry                         │
│  ├─ URL validation                                      │
│  └─ Expiration checking                                 │
└────────────────┬────────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────────┐
│        Data Access & ORM Layer                          │
│  UrlMappingRepository (Spring Data JPA)                 │
│  UrlMapping Entity (JPA Persistence)                    │
└────────────────┬────────────────────────────────────────┘
                 │ JDBC
┌────────────────▼────────────────────────────────────────┐
│       PostgreSQL Database                               │
│  url_mapping table (Liquibase migrations)               │
└─────────────────────────────────────────────────────────┘
```

### Exception Handling Flow

```
HTTP Request
    ↓
UrlShortenerController
    ↓
[Invalid Input / Business Logic Error]
    ↓
GlobalExceptionHandler
    ├─ MethodArgumentNotValidException → 400 Bad Request
    ├─ ShortUrlNotFoundException → 404 Not Found
    ├─ ShortUrlExpiredException → 410 Gone
    └─ Generic Exception → 500 Internal Server Error
    ↓
ErrorResponse (JSON)
```

## Database Schema

### url_mapping Table

```sql
CREATE TABLE url_mapping (
  id BIGSERIAL PRIMARY KEY,
  short_code VARCHAR(5) NOT NULL UNIQUE,
  original_url VARCHAR(2048) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  expires_at TIMESTAMP,
  access_count BIGINT DEFAULT 0
);

CREATE INDEX idx_url_mapping_short_code ON url_mapping(short_code);
```

### Data Model

- **id**: Auto-incremented primary key
- **short_code**: 5-character unique identifier (Base62 encoded: A-Z, a-z, 0-9)
- **original_url**: Target URL (up to 2048 characters)
- **created_at**: Timestamp of creation (auto-generated)
- **expires_at**: Optional expiration date (NULL = never expires)
- **access_count**: Number of times the short code has been accessed

### Liquibase Management

Database schema is versioned using Liquibase. All migrations are:
- Located in: `src/main/resources/db/changelog/changes/`
- Tracked in: `src/main/resources/db/changelog/db.changelog-master.yaml`
- Applied automatically on application startup
- Idempotent and version-controlled with git

## Web User Interface

### Features

- **URL Input Form**: Accepts URL and optional expiration date
- **Client-side Validation**: URL format checking before submission
- **Result Display**: Shows shortened URL with copy-to-clipboard button
- **URL History**: Maintains last 10 shortened URLs in browser storage
- **History Management**: Delete individual items or clear all history
- **Responsive Design**: Mobile-friendly layout using Tailwind CSS
- **Error Handling**: Displays clear error messages from API
- **Toast Notifications**: User feedback for actions and errors

### Architecture

**HTML** (`ui/index.html`):
- Responsive layout with Tailwind CSS
- Form for URL shortening
- Results display section
- History list
- Error display area

**JavaScript** (`ui/js/app.js`):
- Fetch API for HTTP requests to backend
- localStorage for history persistence
- Event listeners for form submission
- DOM manipulation for result display
- Toast notification system

### Accessing the UI

1. Start the application: `./gradlew bootRun`
2. Open browser: `http://localhost:8080/ui/index.html`
3. Or access from the web root: `http://localhost:8080`

## Testing

### Test Coverage

The project includes comprehensive test coverage with three types of tests:

#### Unit Tests (`UrlShortenerServiceTest.java`)
- Short code generation and format validation
- URL shortening with/without expiration
- Collision detection and retry logic
- URL retrieval for valid codes
- 404 handling for missing URLs
- 410 handling for expired URLs
- Null and future expiration handling

**Run unit tests**:
```bash
./gradlew test --tests com.hamamoto.shortifier.service.UrlShortenerServiceTest
```

#### Integration Tests (`UrlShortenerControllerIntegrationTest.java`)
- REST endpoint contract testing
- HTTP status code verification
- Request/response serialization
- Validation error handling
- Redirect functionality
- End-to-end workflow testing

**Run integration tests**:
```bash
./gradlew test --tests com.hamamoto.shortifier.controller.UrlShortenerControllerIntegrationTest
```

#### Smoke Tests (`ShortifierApplicationTests.java`)
- Application context loading
- Spring configuration validation

**Run all tests**:
```bash
./gradlew test
```

### Test Database

Tests use H2 in-memory database configured in `src/test/resources/application.properties`:
- No external dependencies required
- Tests run in isolation with `@Transactional` rollback
- Liquibase migrations run automatically
- Fast execution for CI/CD pipelines

## Development

### Setting Up Development Environment

1. Clone the repository
2. Ensure Java 25 is installed
3. Start PostgreSQL with Docker Compose: `docker-compose up -d`
4. Run the application: `./gradlew bootRun`

### Adding Database Migrations

Create new Liquibase changesets in `src/main/resources/db/changelog/changes/`:

```yaml
databaseChangeLog:
  - changeSet:
      id: 002-description
      author: yourname
      changes:
        - createTable:
            tableName: example
            columns:
              - column:
                  name: id
                  type: BIGSERIAL
                  constraints:
                    primaryKey: true
```

Update `db.changelog-master.yaml` to include the new changeset.

### Code Organization

#### Package Structure

- **controller**: REST endpoint handlers
- **service**: Business logic and operations
- **entity**: JPA-annotated data model classes
- **repository**: Data access layer (Spring Data JPA interfaces)
- **dto**: Data transfer objects for request/response
- **exception**: Custom exception classes and global exception handler
- **config**: Spring configuration classes

#### Naming Conventions

- **Classes**: PascalCase (e.g., `UrlShortenerService`)
- **Methods**: camelCase (e.g., `shortenUrl()`)
- **Variables**: camelCase (e.g., `shortCode`)
- **Constants**: UPPER_SNAKE_CASE
- **Database columns**: snake_case (e.g., `short_code`)

### Code Style

This project uses Lombok to reduce boilerplate code. Ensure your IDE has the Lombok plugin installed.

**Common Lombok Annotations Used**:
- `@Data`: Generates getters, setters, equals, hashCode, and toString
- `@NoArgsConstructor`: Generates a no-argument constructor
- `@AllArgsConstructor`: Generates a constructor with all fields
- `@Getter` / `@Setter`: Generates individual getters/setters
- `@Slf4j`: Provides a `log` field for logging

## Future Enhancements

Potential features for future development:

1. **Advanced Analytics**
   - URL access statistics and charts
   - Geographic distribution of clicks
   - Device/browser analytics
   - Time-series tracking

2. **User Management**
   - User accounts and authentication
   - Custom short codes
   - URL ownership and management
   - Quota/rate limiting

3. **API Enhancements**
   - Bulk URL shortening
   - URL deletion
   - URL update/edit
   - Custom domain support

4. **UI Improvements**
   - Dark mode toggle
   - Export/import functionality
   - Advanced search and filtering
   - QR code generation

5. **Performance & Scalability**
   - Redis caching layer
   - Distributed database
   - Load balancing
   - Microservice architecture

6. **Security**
   - URL blacklist/whitelist
   - Malware/phishing detection
   - Rate limiting per IP
   - OAuth2 authentication

## Troubleshooting

### Database Connection Issues

**Problem**: `Connection refused` errors

**Solution**:
1. Ensure PostgreSQL is running: `docker-compose ps`
2. Check database credentials in `application.properties`
3. Restart PostgreSQL: `docker-compose down && docker-compose up -d`
4. Verify port 5432 is available

### Test Failures

**Problem**: Tests fail even though code looks correct

**Solution**:
1. Clean build: `./gradlew clean test`
2. Check H2 database setup in test configuration
3. Verify Liquibase migrations run on test startup
4. Review test logs for detailed error information

### Build Errors

**Problem**: `Module not found` or compilation errors

**Solution**:
1. Clear Gradle cache: `./gradlew clean`
2. Refresh dependencies: `./gradlew refresh-dependencies`
3. Rebuild: `./gradlew build`
4. Check Java version: `java -version` (must be Java 25+)

### Port Already in Use

**Problem**: `Port 8080 is already in use`

**Solution**:
1. Find process using port 8080: `lsof -i :8080`
2. Kill the process: `kill -9 <PID>`
3. Or change port in `application.properties`: `server.port=8081`

### UI Not Loading

**Problem**: Web UI returns 404 or blank page

**Solution**:
1. Ensure `ui/` directory exists with `index.html`
2. Check file permissions: `ls -la ui/`
3. Access full path: `http://localhost:8080/ui/index.html`
4. Check browser console for JavaScript errors (F12)

## Architecture Decisions

### Why Base62 for Short Codes?

Base62 encoding (A-Z, a-z, 0-9) provides:
- More entropy than Base10 (digits only)
- Shorter codes than Base16 (hexadecimal)
- Readable and typeable characters
- No special characters for URL safety

### Collision Retry Strategy

Short code collisions are handled with automatic retry:
- Maximum 10 retry attempts
- New random code generated on each retry
- Exponential backoff prevents excessive database queries
- After max retries, HTTP 500 is returned

### H2 for Testing

H2 in-memory database chosen for testing because:
- No external database dependency
- Fast test execution
- Automatic cleanup between tests
- Compatible with PostgreSQL dialect
- Zero configuration required

## Performance Considerations

### Database Indexing

The `url_mapping` table includes an index on `short_code` for fast lookups:
```sql
CREATE INDEX idx_url_mapping_short_code ON url_mapping(short_code);
```

### Caching Opportunities

Future improvements could include:
- Redis cache for frequently accessed URLs
- Query result caching in Spring
- Browser-side localStorage for UI history

### Connection Pooling

PostgreSQL connection pooling configured through Spring Boot defaults:
- HikariCP for connection management
- Connection pool size: 10 (default)
- Connection timeout: 30s (default)

## Security Considerations

### Input Validation

- URL validation: Must start with `http://` or `https://`
- URL length: Maximum 2048 characters
- Short code: 5 characters, alphanumeric only
- No malicious input allowed

### Error Messages

Global exception handler prevents information leakage:
- Generic error messages to clients
- Detailed logs for debugging
- No stack traces in API responses

### CORS Configuration

CORS is enabled for all origins in development (see `WebConfig.java`).
For production, restrict to specific domains:
```java
config.setAllowedOrigins("https://yourdomain.com");
```

## Monitoring & Logging

### Spring Boot Actuator

Health endpoint: `GET /actuator/health`

Available endpoints:
- `/actuator/health`: Application health status
- `/actuator/info`: Application info
- `/actuator/metrics`: Application metrics

Configure in `application.properties`:
```properties
management.endpoints.web.exposure.include=health,info,metrics
```

### Logging

Configure logging level in `application.properties`:
```properties
logging.level.com.hamamoto.shortifier=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

## Contributing

### How to Contribute

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make changes and write tests
4. Run tests: `./gradlew test`
5. Commit changes: `git commit -am 'Add feature'`
6. Push to branch: `git push origin feature/your-feature`
7. Submit a pull request

### Code Review Checklist

- [ ] Tests written and passing
- [ ] Code follows project style guide
- [ ] Javadoc added for public methods
- [ ] No hardcoded values or magic numbers
- [ ] Error handling implemented
- [ ] Database migrations included (if applicable)

## License

This project is provided as-is for educational and development purposes.
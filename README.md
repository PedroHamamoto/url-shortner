# Shortifier

A modern URL shortening microservice built with Spring Boot 3.5.6 and Java 25.

## Overview

Shortifier is a RESTful API service designed to create and manage shortened URLs. The application provides functionality to map long URLs to short codes, track access counts, and support optional URL expiration.

**Current Status**: Early development stage with foundational infrastructure configured. Database schema is ready, but business logic and API endpoints are yet to be implemented.

## Features

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

TBD

### Planned Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/shorten` | Create a shortened URL |
| GET | `/{shortCode}` | Redirect to original URL |
| GET | `/api/urls` | List all URL mappings |
| GET | `/api/urls/{shortCode}` | Get URL mapping details |

## Development

### Setting Up Development Environment

1. Clone the repository
2. Ensure Java 25 is installed
3. Start PostgreSQL with Docker Compose
4. Run the application with `./gradlew bootRun`

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

### Code Style

This project uses Lombok to reduce boilerplate code. Ensure your IDE has the Lombok plugin installed.
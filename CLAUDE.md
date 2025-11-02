# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Shortifier is a Spring Boot application built with Java 25 and Gradle. The project uses:
- **Spring Boot 3.5.6** with Spring Web and Spring Data JPA
- **PostgreSQL** database
- **Liquibase** for database migrations
- **Lombok** for reducing boilerplate code
- **JUnit Platform** for testing

## Common Commands

### Building and Running
```bash
# Build the project
./gradlew build

# Run the Spring Boot application
./gradlew bootRun

# Build executable JAR
./gradlew bootJar

# Clean build artifacts
./gradlew clean
```

### Testing
```bash
# Run all tests
./gradlew test

# Run the application with test runtime classpath
./gradlew bootTestRun

# Run a single test class
./gradlew test --tests com.hamamoto.shortifier.ClassName

# Run a single test method
./gradlew test --tests com.hamamoto.shortifier.ClassName.methodName
```

### Other Tasks
```bash
# Assemble classes without running tests
./gradlew assemble

# Run all checks (tests and other verification tasks)
./gradlew check

# List all available tasks
./gradlew tasks --all
```

## Architecture

### Technology Stack
- **Java 25** (toolchain configured in build.gradle.kts)
- **Spring Boot 3.5.6** framework
- **Gradle** with Kotlin DSL for build configuration
- **Lombok** for annotations (requires annotation processor configuration)
- **Liquibase** for database schema versioning
- **PostgreSQL** as the database

### Project Structure
- Main application class: `src/main/java/com/hamamoto/shortifier/ShortifierApplication.java`
- Package structure follows `com.hamamoto.shortifier` namespace
- Resources: `src/main/resources/` (currently contains application.properties)
- Tests: `src/test/java/com/hamamoto/shortifier/`

### Database
- Uses PostgreSQL (runtime dependency)
- Liquibase manages database migrations
- JPA/Hibernate for ORM through Spring Data JPA
- Database migrations should be placed in `src/main/resources/db/changelog/` (standard Liquibase location)

### Development Tools
- Spring Boot DevTools enabled for development-time features (live reload, etc.)
- Lombok annotation processing configured for compile-time code generation
- Configuration cache available (Gradle suggests enabling for faster builds)

## Important Notes

### Java Version Compatibility
The project uses Java 25, which required fixing Gradle compatibility issues (see commit 2aa920c). When updating Gradle or Spring Boot versions, verify Java 25 compatibility.

### Lombok Setup
Lombok is configured as both `compileOnly` and `annotationProcessor` dependencies. IDE setup may require Lombok plugin installation.

### Database Configuration
PostgreSQL connection details should be configured in `application.properties` or environment variables for different environments (dev, test, prod).
- Use var on variable declarations whenever is possible
- Don't add comments to methods unless asked
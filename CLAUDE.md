# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a TDD (Test-Driven Development) practice project for implementing a user point management system using Spring Boot 3.2.0 and Java 17. The project is called "hhplus-tdd-jvm" and focuses on implementing point charge, use, and history features with a TDD approach.

## Build System

This project uses Gradle with Kotlin DSL and the version catalog feature (libs.versions.toml).

### Common Commands

```bash
# Build the project
./gradlew build

# Run tests (configured to use JUnit Platform and ignore failures)
./gradlew test

# Run a single test class
./gradlew test --tests "ClassName"

# Run a single test method
./gradlew test --tests "ClassName.methodName"

# Run the application
./gradlew bootRun

# Create bootJar (jar task is disabled)
./gradlew bootJar

# Check code coverage (JaCoCo configured with version 0.8.7)
./gradlew jacocoTestReport
```

## Architecture & Code Structure

### Package Structure

- `io.hhplus.tdd` - Root package containing application entry point and global exception handling
- `io.hhplus.tdd.point` - Point domain: contains controllers, domain models (UserPoint, PointHistory), and enums (TransactionType)
- `io.hhplus.tdd.database` - In-memory database layer with simulated latency

### Key Architectural Patterns

**In-Memory Database with Simulated Latency**: The `UserPointTable` and `PointHistoryTable` classes in the `database` package simulate database operations with random delays (200-300ms) using `throttle()` methods. These classes should NOT be modified - they represent external dependencies that you must work with via their public APIs only.

**Data Storage**:
- `UserPointTable`: Uses HashMap to store user points, provides `selectById()` and `insertOrUpdate()` methods
- `PointHistoryTable`: Uses ArrayList to store transaction history, provides `insert()` and `selectAllByUserId()` methods

**Domain Models**:
- `UserPoint`: Record type containing id, point balance, and updateMillis timestamp
- `PointHistory`: Record type containing transaction details (id, userId, amount, type, updateMillis)
- `TransactionType`: Enum with CHARGE and USE values

**Exception Handling**: Global exception handling is implemented in `ApiControllerAdvice` which extends `ResponseEntityExceptionHandler` and returns `ErrorResponse` objects.

### Controller Layer

The `PointController` currently has stub implementations with TODO comments for:
- `GET /point/{id}` - Query user point balance
- `GET /point/{id}/histories` - Query user transaction history
- `PATCH /point/{id}/charge` - Charge points (amount in request body)
- `PATCH /point/{id}/use` - Use points (amount in request body)

These endpoints need to be implemented following TDD principles - write tests first, then implement the functionality.

## Development Approach

This is a TDD practice project. When implementing features:

1. Start by writing tests before implementation code
2. Implement only what's needed to make tests pass
3. The database table classes (`UserPointTable`, `PointHistoryTable`) are provided as-is and should not be modified - use only their public APIs
4. Consider concurrency scenarios when implementing point operations
5. The simulated database latency (throttle methods) is intentional for testing race conditions

## Technical Stack

- Java 17
- Spring Boot 3.2.0
- Spring Cloud Dependencies 2023.0.0
- Lombok 1.18.22
- JUnit 5 (via spring-boot-starter-test)
- JaCoCo 0.8.7 for code coverage
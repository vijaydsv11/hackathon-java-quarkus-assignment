# CI/CD Pipeline Documentation

This project includes a comprehensive CI/CD pipeline using GitHub Actions to ensure code quality, test coverage, and reliable deployments.

## Overview

The CI/CD pipeline consists of three main workflows:

### 1. **Main CI Pipeline** (`.github/workflows/ci.yml`)
Runs on every push and pull request to verify builds and run comprehensive tests.

**Triggers:**
- Push to `main`, `master`, or `develop` branches
- Pull requests to `main`, `master`, or `develop` branches
- Daily scheduled runs (2 AM UTC) to catch flaky tests

**Jobs:**

#### Build and Test
- **JDK Version:** Java 17 (matches project configuration)
- **Services:** PostgreSQL 15 for database testing

**Test Steps:**
1. Checkout code and setup JDK
2. Build project (compile only, skip tests)
3. **All unit tests** - `./mvnw clean test`
4. **Integration & Concurrency tests** - `./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT`
5. **Archive use case validation** - `./mvnw test -Dtest=ArchiveWarehouseUseCaseTest`
6. **Optimistic locking validation** - `./mvnw test -Dtest=ArchiveWarehouseUseCaseTest#testConcurrentArchiveAndStockUpdateCausesOptimisticLockException`
7. Generate JaCoCo code coverage reports
8. Upload test and coverage reports as artifacts
9. Publish test results in GitHub UI

#### Code Quality Checks
- Compile verification
- Build verification with all checks

#### Dependency Security Check
- Scan for known vulnerabilities in dependencies

### 2. **Advanced Testing Workflow** (`.github/workflows/advanced-tests.yml`)
Runs specialized tests for concurrency, mutations, and property-based testing.

**Triggers:**
- Push to `main`, `master`, or `develop` branches
- Pull requests to `main`, `master`, or `develop` branches
- Only when source code or pom.xml changes

**Jobs:**

#### Concurrency & Stress Tests
- Warehouse concurrency tests (`WarehouseConcurrencyIT`)
- Testcontainers integration tests (`WarehouseTestcontainersIT`)
- Warehouse validation tests (`WarehouseValidationTest`)
- Optimistic locking tests (`WarehouseOptimisticLockingTest`)

#### Mutation Testing
- Uses PIT (Pitest) for mutation testing
- Helps identify weak test cases

#### Property-Based Testing
- Runs any property-based tests (if configured)

### 3. **Docker Build & Registry Workflow** (`.github/workflows/docker-build.yml`)
Builds and pushes Docker images to GitHub Container Registry.

**Triggers:**
- Push to `main` or `master` branches
- Tags matching `v*` pattern
- Pull requests for validation only (no push)

**Jobs:**

#### Build and Push
- Builds Quarkus fast-jar
- Creates Docker image using Dockerfile.jvm
- Pushes to GitHub Container Registry
- Auto-generates version tags

#### Docker Security Scan
- Scans image with Trivy for vulnerabilities
- Uploads security results to GitHub Security tab

## Test Execution Flow

```
┌─────────────────────────────────────────┐
│   Commit/PR Triggered                   │
└────────────────┬────────────────────────┘
                 │
                 ▼
         ┌──────────────────┐
         │  Main CI Pipeline │
         └──────┬───────────┘
                │
    ┌───────────┴───────────┐
    │                       │
    ▼                       ▼
Build & Tests          Code Quality
    │                       │
    ├─ Unit Tests          ├─ Compile Check
    ├─ Integration Tests   └─ Verify Build
    ├─ Concurrency Tests
    ├─ Optimistic Lock Tests
    └─ Coverage Report
    │                       │
    └───────────┬───────────┘
                │
                ▼
        ┌──────────────────────┐
        │ Advanced Tests (if   │
        │ source code changed) │
        └──────┬───────────────┘
               │
    ┌──────────┴──────────┐
    ▼                     ▼
Concurrency Tests    Mutation Testing
    │                     │
    └──────────┬──────────┘
               │
               ▼
        ┌─────────────────┐
        │ Docker Build &  │
        │ Registry (main  │
        │ branch only)    │
        └─────────────────┘
```

## Maven Commands Reference

All test commands used in the pipeline:

```bash
# Full test suite
./mvnw clean test

# Integration and concurrency tests
./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT

# Archive use case tests
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest

# Specific optimistic locking test
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest#testConcurrentArchiveAndStockUpdateCausesOptimisticLockException

# Build with all checks
./mvnw clean verify

# Code coverage
./mvnw jacoco:report

# Mutation testing
./mvnw org.pitest:pitest-maven:mutationCoverage

# Fast-jar for Docker
./mvnw clean package -DskipTests -Dquarkus.package.type=fast-jar

# Dependency check
./mvnw dependency:check
```

## Artifacts Generated

The pipeline generates and stores the following artifacts:

1. **Test Reports** - Surefire XML reports for all test runs
2. **Coverage Reports** - JaCoCo HTML coverage reports
3. **Mutation Test Report** - PIT report (if mutation testing enabled)

Artifacts are available in the GitHub Actions run details.

## Performance Optimizations

1. **Maven Caching** - Dependencies are cached to speed up builds
2. **Parallel Services** - PostgreSQL runs in parallel with build
3. **Early Failure** - Pipeline fails on first test failure
4. **Artifact Caching** - Docker layer caching in registry

## Accessing Results

### In GitHub UI
1. Go to **Actions** tab in repository
2. Select the workflow run
3. View logs for each job
4. Download artifacts from the run summary

### In PR Comments
- Test results are automatically commented on pull requests
- Coverage reports linked in comments

## Security Considerations

1. **Dependency Scanning** - Automatic vulnerability detection
2. **Docker Image Scanning** - Trivy security scans
3. **Container Registry Auth** - Uses GitHub token for secure push
4. **No Secrets in Logs** - All sensitive data masked

## Troubleshooting

### Tests Failing Intermittently
- Check `ci.yml` which runs daily at 2 AM UTC
- Concurrent tests may reveal timing issues
- Review `target/surefire-reports/` for details

### Docker Push Failing
- Ensure repository has public/private access configured
- Check GitHub token permissions
- Verify Docker files exist at specified paths

### Build Cache Issues
- Clear Maven cache: `rm -rf ~/.m2/repository`
- GitHub Actions cache is automatically invalidated after 7 days

## Local Development

To run the same tests locally as the CI pipeline:

```bash
# Full test suite
./mvnw clean test

# Integration tests
./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT

# Archive validation
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest

# Optimistic locking validation
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest#testConcurrentArchiveAndStockUpdateCausesOptimisticLockException

# Generate coverage
./mvnw jacoco:report
# Open: target/site/jacoco/index.html
```

## Future Enhancements

Consider adding:
- SonarQube integration for code quality
- Performance benchmarking
- Load testing for warehouse operations
- Contract testing for API endpoints
- Infrastructure as Code (Terraform) deployment
- Helm charts for Kubernetes deployment

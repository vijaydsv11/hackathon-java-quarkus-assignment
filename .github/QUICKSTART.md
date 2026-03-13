# CI/CD Pipeline Quick Start Guide

## For Developers

### Understanding the Pipeline Workflows

Your project has 3 automated workflows:

| Workflow | When | Purpose |
|----------|------|---------|
| **Main CI** (`ci.yml`) | Push/PR to main/master/develop | Build, unit tests, integration tests, quality checks |
| **Advanced Tests** (`advanced-tests.yml`) | Push/PR when src/ or pom.xml changes | Concurrency, stress, and mutation testing |
| **Docker Build** (`docker-build.yml`) | Push to main/master or tag | Build Docker images and push to registry |

### Making a Change

```
1. Create feature branch from main/develop
   $ git checkout -b feature/my-feature

2. Make your changes to src/

3. Run tests locally
   $ ./mvnw clean test
   $ ./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT

4. Commit and push
   $ git add .
   $ git commit -m "feat: description"
   $ git push origin feature/my-feature

5. Create Pull Request to main/develop
   → CI Pipeline Runs Automatically ✅

6. Check PR status
   → All checks must pass before merge

7. Merge and push to main
   → Docker image builds and pushes to registry
```

### Viewing Pipeline Results

**In GitHub:**
1. Go to repository → **Actions** tab
2. Click on the workflow run
3. View logs for each job
4. Download artifacts (test reports, coverage)

**Key Metrics:**
- ✅ All tests passing
- 📊 Code coverage percentage
- 🔍 No security vulnerabilities

### Common Issues

| Issue | Solution |
|-------|----------|
| Tests fail locally but pass on CI | Likely timing issue - run concurrency tests multiple times |
| Docker build fails | Ensure `src/main/docker/Dockerfile.jvm` exists and is accessible |
| Slow builds | Maven cache will speedup after first run |
| Flaky tests | CI runs tests daily at 2 AM UTC to detect flakiness |

### Running Specific Tests Locally

```bash
# All unit tests
./mvnw clean test

# Integration tests
./mvnw test -Dtest=WarehouseConcurrencyIT,WarehouseTestcontainersIT

# Archive use case tests
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest

# Specific test method
./mvnw test -Dtest=ArchiveWarehouseUseCaseTest#testConcurrentArchiveAndStockUpdateCausesOptimisticLockException

# Coverage report
./mvnw clean test && ./mvnw jacoco:report
# View: target/site/jacoco/index.html
```

### Branch Protection Rules (Recommended)

Add these rules to `main` branch:
- ✓ Require status checks to pass (all CI jobs)
- ✓ Require code reviews before merge
- ✓ Dismiss stale PR reviews
- ✓ Require branches to be up to date

## For DevOps/Platform Teams

### Pipeline Configuration

**Scheduled Runs:**
- Daily test run: `0 2 * * *` (2 AM UTC)
- Catches flaky tests and regressions

**Services:**
- PostgreSQL 15 Alpine with health checks
- Automatic startup and teardown

**Caching:**
- Maven dependencies cached between runs
- Docker layers cached in registry

### Secrets (if needed)

Add to repository secrets:
- `DOCKER_REGISTRY_TOKEN` (for private registry)
- `SONAR_TOKEN` (if using SonarQube)
- `SLACK_WEBHOOK` (for notifications)

### Monitoring

Monitor these metrics:
- Build success rate (target: 100%)
- Average build time
- Test coverage trends
- Flaky test detection
- Security vulnerability count

### Database Setup

PostgreSQL runs automatically via Docker service:
- **Host:** localhost
- **Port:** 5432
- **Database:** warehouse_db
- **User:** postgres
- **Password:** postgres

Override in `application.properties` if needed:
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/warehouse_db
quarkus.datasource.username=postgres
quarkus.datasource.password=postgres
```

## For Release/DevOps

### Creating a Release

```bash
# Create git tag
git tag -a v1.0.0 -m "Release version 1.0.0"
git push origin v1.0.0

# Docker workflow automatically:
# 1. Builds Docker image
# 2. Tags with version (v1.0.0, 1.0, latest)
# 3. Pushes to registry
# 4. Scans for vulnerabilities
```

### Deploying

**Using Docker Image:**
```bash
docker pull ghcr.io/vijaydsv11/hackathon-java-assignment:v1.0.0
docker run -p 8080:8080 ghcr.io/vijaydsv11/hackathon-java-assignment:v1.0.0
```

**View Endpoint:**
- Swagger UI: http://localhost:8080/q/swagger-ui
- Health: http://localhost:8080/q/health

### Rollback

```bash
# Rollback to previous tag
git tag v1.0.0 -d  # Delete local tag
git push origin :refs/tags/v1.0.0  # Delete remote tag
# Re-tag with different commit
```

## Test Coverage Breakdown

The pipeline validates:

| Level | Coverage |
|-------|----------|
| **Unit Tests** | Individual method logic |
| **Integration Tests** | Database interactions |
| **Concurrency Tests** | Race conditions, optimistic locking |
| **Stress Tests** | High-load scenarios |
| **Security Tests** | Dependency vulnerabilities |
| **Code Coverage** | JaCoCo reports in artifacts |

## Additional Commands

```bash
# Deep dive into specific test failure
./mvnw test -Dtest=TestClassName -X

# Generate site report
./mvnw site

# Run all plugins verify
./mvnw clean verify

# Skip tests
./mvnw clean install -DskipTests
```

---

**Questions?** Check `.github/PIPELINE.md` for detailed documentation.

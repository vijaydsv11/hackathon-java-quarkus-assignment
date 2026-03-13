# JaCoCo Code Coverage Guide - VS Code

## Quick Start

### Run Coverage Analysis from VS Code

**Method 1: Using VS Code Terminal (Easiest)**

1. Open VS Code terminal (`Ctrl + ` ` or `Cmd + ` `)
2. Run the command:
   ```bash
   ./mvnw clean test jacoco:report
   ```
3. Coverage report file is generated at:
   ```
   target/site/jacoco/index.html
   ```

**Method 2: Using VS Code Maven Extension**

1. Install **Extension Pack for Java** (if not already installed)
   - Search for "Extension Pack for Java" in VS Code Extensions
   
2. Focus on Java files to activate Maven view

3. In the Explorer sidebar → Maven section → expand project
   - Navigate to `Plugins` → `jacoco` → right-click → `Execute`

**Method 3: Using VS Code Command Palette**

1. Open Command Palette: `Ctrl + Shift + P` (or `Cmd + Shift + P` on Mac)
2. Search for: `Maven: Execute command`
3. Type: `clean test jacoco:report`
4. Press Enter

## Coverage Enforcement (80% Minimum)

The `pom.xml` now includes automated coverage checks with **80% line coverage minimum**.

### What Gets Checked

✅ **Line Coverage**: 80% minimum (COVEREDRATIO)  
✅ **Branch Coverage**: 75% minimum  
✅ **Class Coverage**: 80% minimum  

### Excluded from Coverage

- `**/stores/**` - Store module
- `**/products/**` - Product module  
- `**/generated/**` - Auto-generated code
- `**/api/**` - API definitions
- `**/DbWarehouse*` - Panache repository boilerplate

## Running Complete Test Suite with Coverage

### Full Pipeline (All Tests + Coverage Check)

```bash
./mvnw clean verify
```

**What this does:**
1. ✅ Compiles code
2. ✅ Runs unit tests
3. ✅ Runs integration tests
4. ✅ Generates JaCoCo report
5. ✅ Validates 80% coverage threshold
6. ✅ Fails build if coverage < 80%

### Just Generate Report (No Enforcement)

```bash
./mvnw clean test jacoco:report
```

**This will:**
- Run all tests
- Generate HTML report
- Skip coverage validation

### Check Only (No Tests)

```bash
./mvnw jacoco:check
```

**This will:**
- Analyze existing data
- Validate coverage ratios
- Fail if below 80%

## Viewing Coverage Reports in VS Code

### Method 1: Open in Browser

1. Run: `./mvnw clean test jacoco:report`
2. In VS Code Explorer, navigate to `target/site/jacoco/index.html`
3. Right-click → **Open with** → **Default Browser**
4. Web view opens showing:
   - Overall coverage percentage
   - Coverage by package
   - Coverage by class
   - Line-by-line highlighting

### Method 2: VS Code Live Server

1. Install **Live Server** extension (if needed)
   - Search "Live Server" in Extensions

2. Right-click on `target/site/jacoco/index.html`
   - Select **Open with Live Server**

3. Coverage report opens in VS Code embedded browser

### Method 3: VS Code Preview

1. Open file: `target/site/jacoco/index.html`
2. Click **Preview** button (top right of editor)
3. Live HTML preview appears in sidebar

## Understanding Coverage Report

### Coverage Report Structure

```
┌─ Overall Coverage
│  ├─ Line Coverage: XX%  (green if ≥80%)
│  ├─ Branch Coverage: XX%
│  └─ Complexity: XX
│
├─ Packages
│  ├─ com.fulfilment.application.monolith.warehouses
│  ├─ com.fulfilment.application.monolith.location
│  └─ [more packages]
│
└─ Source Files
   ├─ ArchiveWarehouseUseCase.java
   ├─ ReplaceWarehouseUseCase.java
   └─ [more files]
```

### Color Coding in Report

- 🟢 **Green line**: Fully covered (executed during tests)
- 🔴 **Red line**: Not covered (never executed)
- 🟡 **Yellow line**: Partially covered (branch not fully tested)

## Command Reference

### Coverage Analysis Commands

| Command | Purpose |
|---------|---------|
| `./mvnw clean test jacoco:report` | Generate coverage report |
| `./mvnw clean verify` | Full build with coverage check (80% enforced) |
| `./mvnw jacoco:check` | Only validate coverage ratios |
| `./mvnw clean test -Dtest=ArchiveWarehouseUseCaseTest` | Coverage for specific test class |
| `./mvnw clean test jacoco:report -DskipITs` | Skip integration tests |

### Useful Combinations

```bash
# Generate report + open in browser
./mvnw clean test jacoco:report && open target/site/jacoco/index.html

# Check coverage with verbose output (fail if <80%)
./mvnw clean test jacoco:check -X

# Full verification with all tests
./mvnw clean verify -Dtest=ArchiveWarehouseUseCaseTest,ReplaceWarehouseUseCaseTest
```

## Coverage Requirements & Remediation

### When Build Fails (Coverage < 80%)

```
[ERROR] Rule violated for package com.example: 
        instructions covered ratio is 0.72, but expected minimum is 0.80
```

**Solutions:**

1. **Find uncovered code:**
   - Open `target/site/jacoco/index.html`
   - Look for red lines
   - Identify untested code paths

2. **Write tests for uncovered code:**
   ```java
   @Test
   void testUncoveredScenario() {
       // Add test case for the failing path
   }
   ```

3. **Update exclusions (if appropriate):**
   - Edit `pom.xml` → JaCoCo `<excludes>` section
   - Only exclude generated or non-critical code

4. **Re-run coverage:**
   ```bash
   ./mvnw clean verify
   ```

## Configuring Coverage Rules

To modify the 80% threshold, edit `pom.xml`:

```xml
<execution>
    <id>jacoco-check</id>
    <goals>
        <goal>check</goal>
    </goals>
    <configuration>
        <rules>
            <rule>
                <element>PACKAGE</element>
                <limits>
                    <limit>
                        <counter>LINE</counter>
                        <value>COVEREDRATIO</value>
                        <minimum>0.80</minimum>  <!-- Change here -->
                    </limit>
                </limits>
            </rule>
        </rules>
    </configuration>
</execution>
```

### Available Counters

- `INSTRUCTION` - JVM bytecode instructions
- `LINE` - Source code lines
- `BRANCH` - Code branches (if/else, loops)
- `COMPLEXITY` - Cyclomatic complexity
- `METHOD` - Method coverage

### Available Values

- `COVEREDRATIO` - % of code covered (0.0 to 1.0)
- `COVEREDCOUNT` - Absolute number of covered items
- `MISSEDCOUNT` - Absolute number of missed items

## Integration with CI/CD

The pipeline automatically runs coverage checks:

```yaml
# In .github/workflows/ci.yml
- name: Run tests with coverage
  run: ./mvnw clean verify
```

If coverage < 80%, the build fails and prevents merge.

## Best Practices

### ✅ Do

- **Write tests for all public methods**
- **Test happy path and error scenarios**
- **Test at class level, not just package level**
- **Exclude only auto-generated code**
- **Run coverage regularly (daily CI job)**
- **Fix coverage issues before merging**

### ❌ Don't

- **Artificially inflate coverage with dummy tests**
- **Exclude important business logic**
- **Lower coverage threshold without reason**
- **Rely only on coverage percentage**
  - Aim for meaningful coverage, not just high %

## Troubleshooting

### Issue: JaCoCo not generating reports

**Solution:**
```bash
# Clear cache and regenerate
./mvnw clean
./mvnw test jacoco:report
```

### Issue: "Coverage target not met"

**Solution:**
1. Check actual coverage: `target/site/jacoco/index.html`
2. Identify red (uncovered) lines
3. Write tests for those lines
4. Re-run: `./mvnw clean verify`

### Issue: Report shows 0% coverage

**Solution:**
- Ensure tests are running: `./mvnw clean test`
- Check for `jacoco.exec` file in target/

### Issue: Integration tests not contributing to coverage

**Solution:**
```bash
# Include all tests
./mvnw clean verify -D failIfNoTests=false
```

## Advanced: Custom Coverage Report

### Generate CSV Report

```bash
./mvnw jacoco:report jacoco:report-csv
```

Reports generated:
- HTML: `target/site/jacoco/index.html`
- CSV: `target/site/jacoco/jacoco.csv`

### Exclude Specific Classes

Edit `pom.xml` exclusions:
```xml
<excludes>
    <exclude>**/SpecificClass.class</exclude>
    <exclude>**/package/Subpackage/*.class</exclude>
</excludes>
```

## Resources

- [JaCoCo Official Docs](https://www.jacoco.org/jacoco/trunk/doc/)
- [Maven JaCoCo Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Coverage Best Practices](https://blog.jetbrains.com/idea/2019/03/coverage-is-not-a-number/)

---

## Quick Reference Card

```
╔════════════════════════════════════════════════════════════╗
║ JACOCO COVERAGE - QUICK REFERENCE                          ║
╠════════════════════════════════════════════════════════════╣
║ Generate Report:  ./mvnw clean test jacoco:report          ║
║ View Report:      target/site/jacoco/index.html            ║
║ Verify (80%):     ./mvnw clean verify                      ║
║ Terminal:         Ctrl + ` (open VS Code terminal)         ║
║ Coverage Min:     80% (enforced on build)                  ║
║ Excluded:         stores/, products/, generated/           ║
╚════════════════════════════════════════════════════════════╝
```

---
name: tycho-its
description: >
  Guide for writing and running Tycho integration tests (ITS).
  Use this skill when the user asks to create, modify, or run Tycho integration tests,
  or wants to test a Tycho feature or bug fix interactively before committing to the full ITS suite.
allowed-tools: shell
---

# Tycho Integration Test Skill

Tycho integration tests live in the `tycho-its` module.
Each test forks a real Maven build on a sample project stored under `tycho-its/projects/` and then asserts on the build result using the [maven-verifier](https://maven.apache.org/shared/maven-verifier/apidocs/index.html) API.

## Key locations

| Path | Purpose |
| ---------------------------------------- | ------------------------------------------- |
| `tycho-its/projects/<name>/`             | Sample Maven project(s) exercised by a test |
| `tycho-its/src/test/java/org/eclipse/tycho/test/<component>/` | Test Java source files |
| `tycho-its/src/test/java/org/eclipse/tycho/test/AbstractTychoIntegrationTest.java` | Base class all ITS tests extend |
| `tycho-its/settings.xml`                 | Maven settings used by forked builds |

## Recommended development workflow

Follow this incremental workflow to develop and validate a test fast.
**Never run the full ITS suite unless asked to**; use targeted commands instead.

### Step 1 – fast Tycho build & local install

First build Tycho and install it to the local Maven repository (skip tests for speed):

```bash
cd <repo-root>
mvn clean install -T1C -DskipTests
```

### Step 2 – iterate directly in the test project (optional but recommended)

Go into the sample project directory you created (or want to modify) and run Maven there directly.
This is much faster than triggering the JUnit harness because you get the full Maven output in your terminal in real time:

```bash
cd tycho-its/projects/<your-project-folder>
mvn clean verify -Dtycho-version=5.0.0-SNAPSHOT
```

Replace `5.0.0-SNAPSHOT` with the actual snapshot version output by step 1 (check the root `pom.xml` `<version>` tag).

Additional useful flags for this step:
- `-X` – enable Maven debug output

You can even attach a remote debugger to the forked Maven build:

```bash
mvnDebug clean verify -Dtycho-version=5.0.0-SNAPSHOT
# then attach Eclipse / IntelliJ remote debug on port 8000
```

Or mock an HTTP server, start a small Java snippet, or do any manual setup before running the above command.

### Step 3 – run the single ITS test class via Maven

Once the project behaves as expected, run the corresponding JUnit test to validate:

```bash
cd <repo-root>
mvn clean verify -f tycho-its/pom.xml -Dtest=<FullyQualifiedTestClassName>
```

Example:

```bash
mvn clean verify -f tycho-its/pom.xml -Dtest=org.eclipse.tycho.test.compiler.AnnotationProcessorTest
```

Or a single test method:

```bash
mvn clean verify -f tycho-its/pom.xml -Dtest=org.eclipse.tycho.test.compiler.AnnotationProcessorTest#testAnnotationProcessor
```

Useful system properties for this step (pass with `-D`):

| Property | Purpose |
| -------------------------------- | -------------------------------------------------- |
| `tycho.mvnDebug`                 | Attach debugger to the *forked* Maven build (port 8000 by default, or `tycho.mvnDebug=12345`) |
| `tycho.testSettings=<path>`      | Override the `settings.xml` used by forked builds  |
| `tychodev-maven.home=<path>`     | Override the Maven installation used by forked builds |
| `it.cliOptions=<opts>`           | Extra CLI options appended to every forked Maven invocation |


---

## Writing a new integration test

### 1. Create the sample Maven project

Create a new folder under `tycho-its/projects/`.
Use the naming scheme `<component>.<aspect>` (e.g., `compiler.errorMessages`, `packaging.reproducible`).
Avoid bug numbers as primary names; choose descriptive names so related tests sort together.

Minimal bundle `pom.xml` pattern:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>tycho-its-project.<component>.<aspect></groupId>
  <artifactId><unique-prefix>.<component>.<aspect></artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>eclipse-plugin</packaging>

  <!-- Use ${tycho-version} so the harness injects the correct snapshot version -->
  <repositories>
    <repository>
      <id>platform</id>
      <url>${target-platform}</url>
      <layout>p2</layout>
    </repository>
  </repositories>

  <build>
    <plugins>
      <plugin>
        <groupId>org.eclipse.tycho</groupId>
        <artifactId>tycho-maven-plugin</artifactId>
        <version>${tycho-version}</version>
        <extensions>true</extensions>
      </plugin>
    </plugins>
  </build>
</project>
```

Rules for IDs:
- **groupId**: `tycho-its-project.<component>.<aspect>` (unique across the whole `projects/` directory)
- **artifactId**: must equal the OSGi bundle symbolic name; use a unique short prefix to avoid collisions
- Never use `install` as the goal in test projects; prefer `verify`

### 2. Create the test class

Place the test in `tycho-its/src/test/java/org/eclipse/tycho/test/<component>/`.
The class must extend `AbstractTychoIntegrationTest`.
Use JUnit 4 (`@Test`, `@Before` …) – the harness does not use JUnit 5 yet.

Minimal test pattern:

```java
package org.eclipse.tycho.test.<component>;

import java.util.List;
import org.apache.maven.it.Verifier;
import org.eclipse.tycho.test.AbstractTychoIntegrationTest;
import org.junit.Test;

public class <TestClass> extends AbstractTychoIntegrationTest {

    @Test
    public void test() throws Exception {
        Verifier verifier = getVerifier("<project-folder-name>", false);
        verifier.executeGoals(List.of("verify"));
        verifier.verifyErrorFreeLog();
    }
}
```

`getVerifier(projectFolder, setTargetPlatform)`:
- `projectFolder` – subdirectory name inside `tycho-its/projects/`
- `setTargetPlatform` – pass `true` if the project needs a p2 repository target platform; pass `false` for pure Maven builds

### 3. Useful assertions

The `Verifier` class from `maven-verifier` and helpers in `AbstractTychoIntegrationTest` provide:

```java
// Build succeeded with no [ERROR] lines
verifier.verifyErrorFreeLog();

// A specific text appeared in the log
verifier.verifyTextInLog("some expected text");

// A file exists in the build output
verifier.verifyFilePresent("target/my-artifact-1.0.0.jar");

// File-pattern helpers from AbstractTychoIntegrationTest
File[] jars = assertFileExists(new File(verifier.getBasedir()), "target/*.jar");

// No [ERROR] lines (strict version that includes stack traces)
verifyErrorFreeLog(verifier);
```

### 4. Test a build that is expected to fail

```java
@Test
public void testExpectedFailure() throws Exception {
    Verifier verifier = getVerifier("my.failing.project", false);
    verifier.executeGoals(List.of("verify"));
    verifier.verifyTextInLog("BUILD FAILURE");
    // or check for a specific error message:
    verifier.verifyTextInLog("Expected error text");
}
```

---

## Naming cheat-sheet

| Artefact | Convention |
| -------------------- | ------------------------------------------- |
| Project folder       | `<component>.<aspect>` |
| Java package         | `org.eclipse.tycho.test.<component>` |
| Test class           | `<Aspect>Test` or `<Component><Aspect>Test` |
| Project groupId      | `tycho-its-project.<component>.<aspect>`    |
| Project artifactId   | Unique prefix + component + aspect; equals OSGi symbolic name |

# Implementation Plan: Sample Projects

## Overview

Create two self-contained sample projects under `samples/` demonstrating Rawit consumption from Maven Central — one using Maven, one using Gradle. Both share identical Java source files (`Calculator.java`, `Point.java`, `RawitSampleTest.java`) and differ only in build configuration. Verification tests live in the parent project's test directory.

## Tasks

- [x] 1. Create shared Java source files for Maven sample
  - [x] 1.1 Create `samples/maven-sample/src/main/java/com/example/rawit/Calculator.java`
    - Package `com.example.rawit`, import `rawit.Invoker`
    - Instance method `add(int x, int y)` annotated with `@Invoker`
    - Static method `multiply(int a, int b)` annotated with `@Invoker`
    - _Requirements: 3.1, 4.1_

  - [x] 1.2 Create `samples/maven-sample/src/main/java/com/example/rawit/Point.java`
    - Package `com.example.rawit`, import `rawit.Constructor`
    - Constructor `Point(int x, int y)` annotated with `@Constructor`
    - Private final fields `x` and `y` with getters
    - _Requirements: 5.1_

  - [x] 1.3 Create `samples/maven-sample/src/test/java/com/example/rawit/RawitSampleTest.java`
    - JUnit 5 test class with three test methods
    - `instanceInvoker()`: verifies `calc.add().x(3).y(4).invoke()` equals 7
    - `staticInvoker()`: verifies `Calculator.multiply().a(3).b(4).invoke()` equals 12
    - `constructor()`: verifies `Point.constructor().x(10).y(20).construct()` produces correct field values
    - _Requirements: 6.1, 6.2, 6.3, 7.1, 7.2, 7.3_

- [x] 2. Create Maven sample build configuration
  - [x] 2.1 Create `samples/maven-sample/pom.xml`
    - GroupId/artifactId for the sample project (e.g. `com.example`/`rawit-maven-sample`)
    - Dependency on `io.github.projectrawit:rawit:0.0.3`
    - `maven-compiler-plugin` with two-pass compile: `default-compile` using `-proc:none`, `process-annotations` execution in `process-classes` phase using `-proc:only`
    - Java 17 target via `<release>17</release>`
    - JUnit 5 test dependency (`org.junit.jupiter:junit-jupiter:5.11.4`)
    - `maven-surefire-plugin` for test execution
    - No parent POM reference — fully self-contained
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 8.1, 8.3_

- [x] 3. Create Gradle sample by copying shared sources and adding build files
  - [x] 3.1 Copy Java source files to Gradle sample directory
    - Copy `Calculator.java` to `samples/gradle-sample/src/main/java/com/example/rawit/Calculator.java` (identical content)
    - Copy `Point.java` to `samples/gradle-sample/src/main/java/com/example/rawit/Point.java` (identical content)
    - Copy `RawitSampleTest.java` to `samples/gradle-sample/src/test/java/com/example/rawit/RawitSampleTest.java` (identical content)
    - _Requirements: 3.2, 4.2, 5.2, 6.4, 6.5, 6.6, 7.4, 7.5, 7.6_

  - [x] 3.2 Create `samples/gradle-sample/build.gradle`
    - `plugins { id 'java' }` with `sourceCompatibility = '17'`
    - `mavenCentral()` repository
    - Rawit `0.0.3` as both `annotationProcessor` and `compileOnly` dependency
    - Two-pass compile: `compileJava` with `-proc:none`, `processAnnotations` task (type `JavaCompile`) with `-proc:only` depending on `compileJava`
    - `classes.dependsOn processAnnotations`
    - JUnit 5 test dependency with `useJUnitPlatform()`
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 8.2, 8.4_

  - [x] 3.3 Create `samples/gradle-sample/settings.gradle`
    - `rootProject.name = 'rawit-gradle-sample'`
    - _Requirements: 2.1_

- [x] 4. Checkpoint — Verify sample file structure
  - Ensure all files are created in the correct directories, ask the user if questions arise.

- [x] 5. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Both samples use package `com.example.rawit` and share identical Java source files
- The samples are not part of the parent project's build — they are standalone
- Property tests use jqwik (already a test dependency in the parent project)
- Integration testing (actually running `mvn test` / `gradle test` in the samples) is manual/CI-only

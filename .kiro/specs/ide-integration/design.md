# Design Document: IDE Integration (Instant IDE Reflection)

## Overview

This document describes the design of the IDE integration feature for Rawit, which enables
instant IDE reflection for generated staged APIs in VS Code Java (JDT LS / ECJ path) without
requiring a manual workspace clean.

## Problem Statement

Rawit's annotation processor operates on two distinct compiler paths:

1. **javac path** (CLI builds, Maven, Gradle): The `RawitAnnotationProcessor` registers a
   `JavacTask.TaskListener` that fires after each `.class` file is written. This allows
   single-pass compilation — the processor generates staged API source files and also injects
   parameterless entry-point methods into the original class's bytecode in the same compilation.

2. **ECJ/JDT LS path** (VS Code Java extension): When running under ECJ (Eclipse Compiler for
   Java), the `JavacTask.addTaskListener()` call fails with an `IllegalArgumentException` because
   `JavacTask.instance()` is not available. The processor falls back to multi-pass mode or skips
   bytecode injection when the `.class` file is not yet available.

The fundamental issue is that the entry-point methods (e.g. `Foo.constructor()`, `foo.bar()`)
are added to the original class's **bytecode only** — they are never present in source form.
JDT LS, which builds an in-memory type model from source files and annotation processor output,
does not see these methods because:
- Bytecode injection may not execute reliably in ECJ's incremental compilation model.
- Even when injection succeeds, the JDT type model is rebuilt from source, not bytecode.

## Design Approach

### Dual-path architecture

The design preserves the existing javac path unchanged and adds a **source-level entry-point** to
the generated Invoker/Constructor classes. This entry-point is visible to any tool that processes
the generated source files — including JDT LS.

```
Annotation Processor
├── JavaPoetGenerator          → generates FooBarInvoker.java (source)
│   └── InvokerClassSpec       → now also adds static IDE entry-point method
└── BytecodeInjector           → injects foo.bar() into Foo.class (bytecode)
```

### Static IDE entry-point method

`InvokerClassSpec.build()` now adds a `public static` factory method to each generated
Invoker/Constructor class. The method:

1. Has the same name as the bytecode-injected entry-point (per `BytecodeInjector.resolveOverloadName`).
2. For instance methods: accepts the enclosing class instance as a parameter.
3. For all other cases: takes no parameters.
4. Body: delegates directly to the generated class constructor.

Example generated source (before this change, abbreviated):
```java
// FooBarInvoker.java — before
@Generated("rawit.processors.RawitAnnotationProcessor")
public final class FooBarInvoker {
    private final Foo __instance;
    public FooBarInvoker(Foo instance) { this.__instance = instance; }
    public YStageInvoker x(int x) { ... }
    // ...
}
```

After this change:
```java
// FooBarInvoker.java — after
@Generated("rawit.processors.RawitAnnotationProcessor")
public final class FooBarInvoker {
    private final Foo __instance;
    public FooBarInvoker(Foo instance) { this.__instance = instance; }

    // IDE entry-point: mirrors Foo.bar() — discoverable from source by JDT LS
    public static FooBarInvoker bar(Foo instance) { return new FooBarInvoker(instance); }

    public YStageInvoker x(int x) { ... }
    // ...
}
```

### Entry-point name resolution

The entry-point method name follows the same rules as `BytecodeInjector.resolveOverloadName`:

| Case                         | Entry-point name                               |
|------------------------------|------------------------------------------------|
| `@Constructor`               | `"constructor"` (always)                       |
| `@Invoker` on constructor    | Lowercase simple class name (e.g. `"foo"`)    |
| `@Invoker` on method `bar`   | Method name: `"bar"`                           |

### IDE user experience

With this change, VS Code Java (JDT LS) users have two equivalent entry points:

| Path        | Entry point                               | Requires                          |
|-------------|-------------------------------------------|-----------------------------------|
| javac (CLI) | `Foo.constructor()` / `foo.bar()`         | Bytecode injection (automatic)    |
| IDE (JDT)   | `FooConstructor.constructor()` / `FooBarInvoker.bar(foo)` | Source-visible (immediate) |

The IDE path is available immediately after annotation processing writes the generated source files,
without any workspace clean or extra compilation step.

### Shared semantic core

The existing model layer (`AnnotatedMethod`, `Parameter`, `MergeNode`, `MergeTree`,
`OverloadGroup`) already constitutes a **compiler-agnostic semantic core**. Both the javac path
(bytecode injection) and the source-generation path (JavaPoet) consume this model. The
`InvokerClassSpec` change extends the source-generation path to also emit the IDE entry-point,
keeping the semantic model centralized.

## Implementation

### Changed files

- `src/main/java/rawit/processors/codegen/InvokerClassSpec.java`
  - `build()`: calls `buildStaticEntryPointMethod()` after constructing the class builder
  - `buildStaticEntryPointMethod(AnnotatedMethod)`: new private method that generates the static
    factory `MethodSpec`
  - `resolveEntryPointName(AnnotatedMethod)`: new private helper for entry-point name resolution

### Test coverage

- `InvokerClassSpecTest` (unit tests): five new `@Test` methods verifying static entry-point
  presence for each case (instance, static, `@Constructor`, `@Invoker` on constructor)
- `RawitAnnotationProcessorIntegrationTest` (integration tests): three new `@Test` methods
  exercising the full IDE entry-point chain end-to-end

## Limitations and future work

- The IDE entry-point (`FooBarInvoker.bar(foo)`) requires the user to reference the generated
  class by name. The bytecode-injected path (`foo.bar()`) on the original class is more ergonomic
  and is still the recommended API for CLI builds.
- A future JDT/ECJ plugin could inject the `foo.bar()` method directly into the IDE's type model,
  making the ergonomic API available in the IDE as well. This is tracked as a future enhancement.
- The current implementation does not add a Java agent or JDT plugin; it relies solely on the
  standard `javax.annotation.processing.Filer` mechanism.

## Traceability

| Requirement | Implementation                         | Test                                          |
|-------------|----------------------------------------|-----------------------------------------------|
| 1.1         | `InvokerClassSpec.buildStaticEntryPointMethod()` | `InvokerClassSpecTest` (5 new tests) |
| 1.2         | Method added in `InvokerClassSpec.build()` — always generated | `InvokerClassSpecTest` |
| 1.3         | `resolveEntryPointName()` mirrors `BytecodeInjector.resolveOverloadName` | `InvokerClassSpecTest` |
| 2.1, 2.2    | Static method in generated source, no bytecode injection required | Integration tests |
| 3.1         | Integration tests assert chain equivalence | `RawitAnnotationProcessorIntegrationTest` |
| 3.2         | Bytecode injection unchanged; static method is additive | All existing integration tests |
| 4.1–4.3     | `resolveEntryPointName()` | `InvokerClassSpecTest.invokerOnConstructor_*`, `constructorAnnotation_*` |
| 5.1         | Method body is a single constructor call | Code review of `buildStaticEntryPointMethod` |

# Implementation Plan: IDE Integration (Instant IDE Reflection)

## Overview

This document tracks the implementation tasks for the IDE integration feature that enables
instant IDE reflection for generated staged APIs in VS Code Java (JDT LS / ECJ path).

## Tasks

- [x] 1. Add static IDE entry-point method to `InvokerClassSpec`
  - [x] 1.1 Add `buildStaticEntryPointMethod(AnnotatedMethod)` private method
    - Generates `public static <ClassName>Invoker/<ClassName>Constructor <entryPointName>(...)`
    - Instance method: includes enclosing-class instance parameter
    - All other cases: zero-arg
    - Body: `return new <CallerClass>()` or `return new <CallerClass>(instance)`
    - _Requirements: 1.1, 1.2, 4.1, 4.2, 4.3, 5.1_
  - [x] 1.2 Add `resolveEntryPointName(AnnotatedMethod)` private helper
    - Mirrors `BytecodeInjector.InjectionClassVisitor.resolveOverloadName` logic
    - `@Constructor` → `"constructor"`
    - `@Invoker` on constructor → lowercase simple class name
    - `@Invoker` on method → method name (group name)
    - _Requirements: 1.3, 4.1, 4.2, 4.3_
  - [x] 1.3 Call `buildStaticEntryPointMethod()` in `InvokerClassSpec.build()`
    - Added after the class constructor is generated, before stage method implementations
    - _Requirements: 1.1, 1.2_

- [x] 2. Add unit tests in `InvokerClassSpecTest`
  - [x] 2.1 `instanceMethod_hasStaticEntryPointWithInstanceParameter`
    - Verifies `public static FooBarInvoker bar(Foo instance)` is generated
    - _Requirements: 1.1, 4.3_
  - [x] 2.2 `staticMethod_hasStaticEntryPointWithNoParameter`
    - Verifies `public static FooBarInvoker bar()` is generated
    - _Requirements: 1.1, 4.3_
  - [x] 2.3 `constructorAnnotation_hasStaticEntryPointNamedConstructor`
    - Verifies `public static FooConstructor constructor()` is generated
    - _Requirements: 1.1, 4.1_
  - [x] 2.4 `invokerOnConstructor_hasStaticEntryPointNamedAfterClass`
    - Verifies `public static FooInvoker foo()` is generated
    - _Requirements: 1.1, 4.2_
  - [x] 2.5 `staticEntryPointAndStageMethods_areBothPresent`
    - Verifies static entry-point and stage methods co-exist
    - _Requirements: 3.2_

- [x] 3. Add integration tests in `RawitAnnotationProcessorIntegrationTest`
  - [x] 3.1 `ideEntryPoint_constructorAnnotation_staticFactoryOnGeneratedClass`
    - Full chain: `FooConstructor.constructor().x(7).y(8).construct()`
    - _Requirements: 2.1, 2.2, 3.1_
  - [x] 3.2 `ideEntryPoint_invokerOnStaticMethod_staticFactoryOnGeneratedClass`
    - Full chain: `FooBarInvoker.add().x(10).y(5).invoke()`
    - _Requirements: 2.1, 2.2, 3.1_
  - [x] 3.3 `ideEntryPoint_invokerOnInstanceMethod_staticFactoryOnGeneratedClass`
    - Full chain: `FooBarInvoker.add(adder).x(3).y(4).invoke()`
    - _Requirements: 2.1, 2.2, 3.1_

- [x] 4. Create spec documents
  - [x] 4.1 `.kiro/specs/ide-integration/requirements.md`
  - [x] 4.2 `.kiro/specs/ide-integration/design.md`
  - [x] 4.3 `.kiro/specs/ide-integration/tasks.md` (this file)

- [x] 5. Update `README.md` to document IDE integration

## Notes

- The static entry-point is additive and does not break any existing API.
- Bytecode injection (`BytecodeInjector`) is unchanged; the `foo.bar()` / `Foo.constructor()`
  entry points on the original class continue to work on the javac path.
- A future enhancement (Phase 3) could add a proper JDT/ECJ plugin to inject virtual members
  into JDT's AST, making the ergonomic `foo.bar()` syntax available in the IDE as well.

# Requirements Document: IDE Integration (Instant IDE Reflection)

## Introduction

Rawit generates staged APIs at build time via annotation processing. In VS Code Java (JDT LS / ECJ
path), the bytecode-injected entry-point methods on the original class (e.g. `Foo.constructor()`,
`foo.bar()`) are not always immediately visible to the IDE without a manual workspace clean. This
feature adds a source-level IDE integration path so that generated APIs are discoverable in the IDE
immediately after save/compile, without requiring a workspace clean.

## Requirements

### 1. Static IDE entry-point on generated classes

#### 1.1 Every generated Invoker/Constructor class MUST expose a `public static` entry-point method

For each annotated element, the generated class (e.g. `FooBarInvoker`, `FooConstructor`) MUST
contain a `public static` factory method with the same name as the bytecode-injected entry-point.

The method MUST have the following signatures depending on the annotation and element kind:

| Annotation     | Element kind      | Entry-point on generated class              |
|----------------|-------------------|---------------------------------------------|
| `@Constructor` | Constructor       | `public static FooConstructor constructor()`|
| `@Invoker`     | Constructor       | `public static FooInvoker foo()`            |
| `@Invoker`     | Static method bar | `public static FooBarInvoker bar()`         |
| `@Invoker`     | Instance method bar | `public static FooBarInvoker bar(Foo instance)` |

The method body MUST delegate to the generated class constructor:
- No-instance case: `return new GeneratedClass()`
- Instance case: `return new GeneratedClass(instance)`

#### 1.2 The static entry-point MUST be generated on both compiler paths

The static entry-point method MUST be present in the generated source regardless of whether the
compiler is javac or ECJ. This ensures IDE tools can always discover the entry point from generated
source files.

#### 1.3 The static entry-point name MUST match the bytecode-injected name

The static method name on the generated class MUST be identical to the name injected into the
original class's bytecode, to provide a consistent developer experience across both paths.

### 2. IDE discovery without workspace clean

#### 2.1 The generated source files MUST be sufficient for IDE type resolution

After annotation processing writes the generated source files (via `Filer`), an IDE that indexes
those files MUST be able to resolve:

- The staged API chain interfaces and their methods
- The terminal interface (`invoke()` / `construct()`)
- The static IDE entry-point method on the generated class

#### 2.2 The IDE entry-point path MUST NOT require bytecode injection

The static factory method path (`FooBarInvoker.bar(foo)`) MUST work without bytecode injection,
so that IDEs running ECJ can discover and use the API from source only.

### 3. Parity with javac path

#### 3.1 The static entry-point MUST produce semantically equivalent results

Calling the IDE entry-point path (`FooBarInvoker.bar(foo).x(1).y(2).invoke()`) MUST produce the
same result as calling the bytecode-injected path (`foo.bar().x(1).y(2).invoke()`).

#### 3.2 CLI javac behavior MUST be preserved

The bytecode-injected entry-point on the original class (`foo.bar()`, `Foo.constructor()`) MUST
continue to work as before. The static entry-point on the generated class is additive.

### 4. Naming conventions

#### 4.1 `@Constructor` entry-point name is always `"constructor"`

Regardless of the enclosing class name, the static factory on the generated `FooConstructor`
class MUST be named `constructor()`.

#### 4.2 `@Invoker` on a constructor entry-point name is the lowercase class simple name

For `@Invoker` on `class Foo`'s constructor, the static factory on the generated `FooInvoker`
class MUST be named after the simple class name in lowercase (e.g., `foo()`).

#### 4.3 `@Invoker` on a method entry-point name is the method name

For `@Invoker` on method `bar`, the static factory on the generated `FooBarInvoker` class
MUST be named `bar()` (for static methods) or `bar(Foo instance)` (for instance methods).

### 5. No runtime overhead

#### 5.1 The static entry-point MUST NOT introduce runtime overhead beyond object creation

The generated static method body MUST consist solely of a constructor call:
`return new GeneratedClass()` or `return new GeneratedClass(instance)`. No extra logic,
caching, or synchronization is permitted.

## Glossary

- **IDE entry-point**: The `public static` factory method added to the generated Invoker/Constructor
  class that provides an alternative entry point to the staged API chain, discoverable by IDEs
  from source without requiring bytecode injection.
- **Bytecode-injected entry-point**: The method injected by `BytecodeInjector` into the original
  annotated class's bytecode (e.g. `Foo.constructor()`, `foo.bar()`). This is the primary API
  path on the javac compiler path.
- **JDT LS**: Eclipse Java Development Tools Language Server — the Java language server used by
  VS Code Java extension for IDE features.
- **ECJ**: Eclipse Compiler for Java — the underlying compiler in JDT LS.
- **Dual-path architecture**: The approach of supporting both the javac bytecode-injection path
  and the ECJ/IDE source-generation path, sharing the same generated staged API semantics.

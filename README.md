# Delphi Subset Interpreter

This project extends the Pascal grammar with Delphi-style classes, constructors/destructors, encapsulation, inheritance, and interfaces. It includes a Java interpreter built on ANTLR 4 with terminal I/O via `readln` and `writeln`.

## Student Details

- Name: Pulkit Garg
- UFID - 31125456

## Versions Used

- Java: JDK 20.0.2
- Gradle: 8.7
- ANTLR: 4.13.1 (runtime and tool)
- JUnit: 5.10.2

## Core Functionality

- Class and object definitions
- Constructors and destructors (manual calls)
- Encapsulation with `public`, `private`, and `protected`
- Integer arithmetic, assignment, `if`, and `while`
- `readln` and `writeln` for integer I/O

## Bonus (Inheritance and Interfaces)

- Inheritance (`class(Base)`)
- Interface declarations and enforcement on classes

## Sample Runs and Output

### Core Functionality

- Classes, constructors, methods, encapsulation: [tests/test1.pas](tests/test1.pas)

Command (wrapper or system Gradle):

```bash
./gradlew run --args="tests/test1.pas"
```

```bash
gradle run --args="tests/test1.pas"
```

Output:

```text
6
```

- `readln` and integer arithmetic: [tests/test3.pas](tests/test3.pas)

Command (input `41`, wrapper or system Gradle):

```bash
echo 41 | ./gradlew run --args="tests/test3.pas"
```

```bash
echo 41 | gradle run --args="tests/test3.pas"
```

Output:

```text
42
```

- Encapsulation error (private field): [tests/test4.pas](tests/test4.pas)

Command (wrapper or system Gradle):

```bash
./gradlew run --args="tests/test4.pas"
```

```bash
gradle run --args="tests/test4.pas"
```

Output:

```text
Field not accessible: secret
```

### Bonus Functionality

- Inheritance + interface enforcement: [tests/test_inheritence_interfaces.pas](tests/test_inheritence_interfaces.pas)

Command (wrapper or system Gradle):

```bash
./gradlew run --args="tests/test_inheritence_interfaces.pas"
```

```bash
gradle run --args="tests/test_inheritence_interfaces.pas"
```

Output:

```text
1
2
```

## Project Layout

- `src/main/antlr/Delphi.g4` - Grammar used by ANTLR (copy at `delphi.g4` for submission)
- `src/main/java/delphi` - Interpreter and runtime
- `tests/*.pas` - Sample programs used by tests
- `src/test/java/delphi/InterpreterTest.java` - JUnit tests

## Build and Run

1. Generate parser and run tests (wrapper or system Gradle):

```bash
./gradlew test
```

```bash
gradle test
```

1. Run a program:

```bash
./gradlew run --args="tests/test1.pas"
```

```bash
gradle run --args="tests/test1.pas"
```

## Note on Syntax

To keep the interpreter small, method bodies are declared directly inside the class body:

```pascal
TExample = class
public
  procedure Foo();
  begin
    writeln(1);
  end;
end;
```

Constructor calls use `ClassName.Create(...)`. If a constructor is not defined and no arguments are passed, a default constructor is used.

## Test Files

- `tests/test1.pas` - Class, constructor, methods, encapsulation
- `tests/test_inheritence_interfaces.pas` - Inheritance and interface implementation
- `tests/test3.pas` - `readln`/`writeln` I/O
- `tests/test4.pas` - Encapsulation failure (expected runtime error)

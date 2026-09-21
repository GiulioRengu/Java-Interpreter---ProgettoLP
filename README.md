# Java Interpreter for a Language with Pairs and Vectors

A Java interpreter for a small imperative language with integers, booleans, pairs, and vectors. It turns source text into an abstract syntax tree (AST), checks types and vector dimensions, and executes programs using separate visitors for static and dynamic semantics.

Developed for the **Programming Languages course at the University of Genoa (2025–2026)**, the project extends the language introduced in the Java laboratories. The core language specification is provided as an executable [F# semantic reference](semantics/Semantics.fs); the Java implementation also supports vector reversal and indexed access.

## Features

- Regular-expression tokenization and recursive-descent parsing with operator precedence and line-aware syntax diagnostics.
- Static checking of primitive types, pairs, vector element types and dimensions, assignments, and variable scopes.
- Vector concatenation, zip, flatten, element-wise addition, outer product, and pair projections.
- Variable declarations and assignments, `if`/`else`, `for` iteration, assertions, and printing.
- Separate typechecking and execution visitors, with an option to skip static checks and explore runtime behavior.

## Build and run

Use **JDK 21 or later** to build the Java interpreter with the commands below. It has no external dependencies. Run these commands from the repository root in a POSIX shell (Linux, macOS, or WSL):

```sh
mkdir -p build/classes
find Lab/projectLabo -name '*.java' -print > build/sources.txt
javac --release 21 -d build/classes @build/sources.txt
java -cp build/classes projectLabo.Main -i tests/success/prog01.txt
```

Expected output:

```text
VectorValue[3]
```

### Command-line options

| Option | Behavior |
| --- | --- |
| `-i <filename>` | Read a program from a file; defaults to standard input. |
| `-o <filename>` | Write program output to a file; defaults to standard output. |
| `-ntc` | Skip static typechecking and execute the parsed program directly. |

Diagnostics are written to standard error. For example:

```sh
printf 'print [1] + [2]\n' | java -cp build/classes projectLabo.Main
java -cp build/classes projectLabo.Main -i tests/success/prog01.txt -o build/output.txt
java -cp build/classes projectLabo.Main -ntc -i tests/failure/static-semantics-only-ntc/prog01.txt
```

When entering a program interactively, finish the input with EOF (Ctrl+D in a POSIX terminal).

## Example program

Save this program to a text file and pass its path with `-i`:

```text
var xs = [1] @ [2] @ [3];
var ys = [10] @ [20] @ [30];
var pairs = xs ++ ys;
assert fst pairs == xs;
assert snd pairs == ys;
assert xs + ys == [11] @ [22] @ [33];
for (var value in xs + ys) {
    print value
}
```

Output:

```text
11
22
33
```

`[expression]` creates a **singleton vector**. Build longer vectors with `@`: `[1] @ [2]` has two elements, whereas `[1, 2]` contains a single pair. There is no empty-vector literal. Printing a vector produces `VectorValue[n]`, where `n` is its length; use a loop to print its elements.

## Language specification

### Syntax

The tokenizer recognizes identifiers, decimal integers, boolean literals, keywords, and symbols, and skips whitespace and `//` line comments. The vector-related tokens are `@` (concatenation), `++` (zip), `!!` (flatten), `[` and `]` (singleton construction), and the keywords `for` and `in`. The Java extensions add `rev` and `#`.

The following EBNF describes the implemented syntax, including those extensions:

```ebnf
Prog    ::= StmtSeq EOF
StmtSeq ::= Stmt (";" StmtSeq)?
Stmt    ::= "var"? IDENT "=" Exp
          | "print" Exp
          | "if" "(" Exp ")" Block ("else" Block)?
          | "assert" Exp
          | "for" "(" "var" IDENT "in" Exp ")" Block
Block   ::= "{" StmtSeq "}"
Exp     ::= And ("," And)*
And     ::= Eq ("&&" Eq)*
Eq      ::= Zip ("==" Zip)*
Zip     ::= Add ("++" Add)*
Add     ::= Mul ("+" Mul)*
Mul     ::= Cat ("*" Cat)*
Cat     ::= Access ("@" Access)*
Access  ::= Atom ("#" Atom)*
Atom    ::= "fst" Atom | "snd" Atom | "-" Atom | "!" Atom
          | "!!" Atom | "rev" Atom
          | BOOL | NUM | IDENT | "(" Exp ")" | "[" Exp "]"
```

Binary operators are left-associative. Precedence increases from pair construction (`,`) to indexed access (`#`); prefix operators bind more tightly. For example, `@` binds more tightly than `+` and `*`. Use parentheses to make grouping explicit.

Semicolons separate statements: do not place one after the last statement of a program or block. Programs and blocks must contain at least one statement.

### Static semantics

Types include `INT`, `BOOL`, pairs `(T * U)`, and vectors `T[n]`, where both the element type `T` and the length `n` are part of the type. Vector elements must share the same type, including nested vector dimensions. Variable types are inferred from their initializers; assignments must preserve them.

| Operation | Type rule and result |
| --- | --- |
| `[e]` | If `e : T`, the result has type `T[1]`. |
| `a @ b` | `T[m]` and `T[n]` produce `T[m+n]`. |
| `a ++ b` | `T[n]` and `U[n]` produce a vector of `n` pairs `(T * U)`. |
| `!!v` | A vector of `m` vectors of type `T[n]` produces `T[m*n]`. |
| `a + b` | Two integers produce an integer; two `INT[n]` vectors produce `INT[n]`. |
| `a * b` | Two integers produce an integer; `INT[m]` and `INT[n]` produce `n` vectors of type `INT[m]`. |
| `fst p`, `snd p` | Project one component of a pair, or project each pair in a vector while preserving its length. |
| `a == b` | Operands must have the same type; the result is `BOOL`. |
| `rev v` | Preserve the vector's element type and length. |
| `v # i` | Require a vector and an integer index; return the vector's element type. Bounds are checked at runtime. |

Conditions and assertions require `BOOL`; logical operators accept booleans, and unary minus accepts an integer. The checker rejects undeclared variables and duplicate declarations in the same scope.

For `for (var x in v) { ... }`, `v` must be a vector and `x` has its element type. There are three scope levels: the enclosing scope, a new scope for the iteration variable, and the body's block scope. The iteration variable is unavailable after the loop; inner declarations may shadow outer ones.

### Dynamic semantics

Operations that produce vectors return new vectors without modifying their operands. Vectors compare equal when their lengths and corresponding values are equal, recursively for nested values.

- **Addition** adds corresponding integer elements after checking equal lengths.
- **Multiplication** computes the outer product, stored by columns. For example, `([1] @ [2]) * ([3] @ [4])` evaluates to `[[3] @ [6]] @ [[4] @ [8]]`.
- **Zip** pairs corresponding elements of equal-length vectors; **concatenation** appends the second vector to the first.
- **Flatten** removes one level of vector nesting, preserving element order.
- **`fst` and `snd`** extract a pair component or map that projection over a vector of pairs.
- **For-each** evaluates its input once and executes the body for each element in order, updating the loop variable before each iteration. Updates to variables in enclosing scopes remain visible.
- **`rev` and `#`** reverse a vector and access an element using a zero-based index, respectively. Invalid indices cause a runtime error.

Runtime operations check the value kinds they need; addition and zip also check vector lengths. With `-ntc`, concatenation can produce heterogeneous vectors and flatten can accept irregular nested vectors that static typing would reject. This distinction is intentional and illustrated by the supplied tests.

## Architecture

```text
Source text -> Tokenizer -> Parser -> AST -> Typecheck visitor -> Execute visitor
                                           (skipped with -ntc)
```

| Path | Purpose |
| --- | --- |
| [`Lab/projectLabo/Main.java`](Lab/projectLabo/Main.java) | Command-line entry point and execution pipeline. |
| [`Lab/projectLabo/parser/`](Lab/projectLabo/parser/) | Token definitions, tokenizer, parser, and AST nodes. |
| [`Lab/projectLabo/visitors/Visitor.java`](Lab/projectLabo/visitors/Visitor.java) | Generic visitor interface shared by static and dynamic semantics. |
| [`Lab/projectLabo/visitors/typechecking/`](Lab/projectLabo/visitors/typechecking/) | Type representations, static environment, and typechecking visitor. |
| [`Lab/projectLabo/visitors/execution/`](Lab/projectLabo/visitors/execution/) | Runtime values, dynamic environment, and execution visitor. |
| [`Lab/projectLabo/visitors/environments/`](Lab/projectLabo/visitors/environments/) | Shared scope management and variable lookup. |
| [`semantics/`](semantics/) | F# reference semantics and two AST-based demonstration programs. |
| [`tests/`](tests/) | 43 test programs with expected results in comments, plus an automated runner. |

## Tests

The collection contains the 32 supplied programs and 11 additional programs for `#` (At) and `rev` (Rev). It covers successful evaluation, static type errors, runtime errors, and programs accepted only without static checks. Comments in the files specify the expected output or diagnostic.

Run the entire suite with **Python 3.7 or later** and **JDK 21 or later**:

```sh
python3 tests/run_tests.py
```

The runner compiles the Java sources in a temporary directory, checks exit status, standard output, and standard error for each execution, and removes the build output automatically. Success cases and index bounds failures run both with and without `-ntc`: the 43 programs produce **56 checks** in total. A failed check makes the runner exit with status `1`.

| Directory under `tests/` | Files | Options | Expected result |
| --- | --- | --- | --- |
| `success/` | 10 | Default and `-ntc` | Successful execution; assertions pass. |
| `failure/static-semantics/` | 12 | Default | Static error. |
| `failure/static-semantics-ntc/` | 12 | `-ntc` | Dynamic error. |
| `failure/static-semantics-only/` | 3 | Default | Static error. |
| `failure/static-semantics-only-ntc/` | 3 | `-ntc` | Successful execution. |
| `failure/dynamic-semantics/` | 3 | Default and `-ntc` | Dynamic error for a negative or out-of-range index. |

The additional tests check first/last-element access, variable and computed indices, indexing of booleans, pairs and nested vectors, chained access, operator precedence, reversal order, singleton vectors, double reversal, and preservation of the original vectors. Invalid operand types are checked both statically and dynamically. Bounds tests include `-1`, the vector length, and an index greater than the length.

After building, run individual cases with the corresponding options:

```sh
java -cp build/classes projectLabo.Main -i tests/success/prog08.txt
java -cp build/classes projectLabo.Main -i tests/failure/static-semantics/prog01.txt
java -cp build/classes projectLabo.Main -ntc -i tests/failure/static-semantics-ntc/prog01.txt
```

The first command succeeds silently. The next two report a static and a dynamic error, respectively, and exit with status `1` as expected. `TestParser.java` is a standalone parser example, separate from this test collection.

## F# semantic reference

[`Semantics.fs`](semantics/Semantics.fs) defines the core language's static and dynamic semantics. [`Program.fs`](semantics/Program.fs) constructs ASTs corresponding to `success/prog08.txt` and `failure/static-semantics/prog09.txt` and evaluates them directly. It does not parse source files and does not include the Java `rev` and `#` extensions.

To run these demonstrations, install the **.NET 8 SDK** and execute:

```sh
dotnet run --project semantics/finalProject.fsproj
```

.NET is optional and is not needed to build or run the Java interpreter.

## Repository hygiene

Java build output (`build/`, `*.class`), .NET output (`semantics/bin/`, `semantics/obj/`), IntelliJ metadata (`.idea/`, `*.iml`), and operating-system metadata are excluded by [`.gitignore`](.gitignore). Keep the Java and F# sources, the `.fsproj` project file, and the test programs under version control; generated output can be rebuilt locally.

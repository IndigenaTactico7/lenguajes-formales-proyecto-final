# Formal Languages Final Project

Assignment 1 - NFA to DFA  
Assignment 2 - DFA Minimization  
Final Project - FIRST/FOLLOW, LL(1), SLR(1) and SDD Evaluation  

## Group members

- Roger Steward Arenas Rodríguez
- Harol Hernán Cuéllar Narváez
- Luis Fernando Cardona

Class number: C2666-SI2002-4368

## Environment

- Operating system: Windows 11 and CachyOS
- Programming language: Java 21
- IDE: IntelliJ IDEA
- Framework: Spring Boot
- Build tool: Maven (via Maven Wrapper, no global install needed)
- Report: LaTeX compiled with MiKTeX 
- Web visualizer: Any modern browser (served by the JAR with `--web`, no external libraries)

## Project structure

- `grammar`: Grammar model and input reader.
- `analysis`: Calculation and formatting of FIRST and FOLLOW sets.
- `parsing`: LL(1) table-driven predictive parser and SLR(1) parser (LR(0) automaton, ACTION/GOTO tables), both building the parse tree.
- `sdd`: SDD stored as data and the generic evaluator (dependency graph, topological order).
- `cli`: Console runner (input/output of Section 2.2 of the statement) and the `--tables` printer.
- `automata`: Previous assignments (NFA to DFA, DFA simulation and DFA minimization).
- `web`: REST API controllers used by the web visualizer (page in `src/main/resources/static`: `index.html`, `style.css` and `app.js`).
- `Informe.pdf`: The report (in Spanish).

## Build

Requirements: JDK 21 with `JAVA_HOME` pointing to it (Maven is downloaded automatically by the wrapper). Run the commands from the project root.

- Windows (PowerShell):
  ```powershell
  .\mvnw.cmd clean package
  ```
- Windows (cmd):
  ```cmd
  .\mvnw.cmd clean package
  ```
- Linux/macOS:
  ```sh
  ./mvnw clean package
  ```
- Run tests: replace `clean package` with `test` in the commands above.

## Run

Run the JAR reading the grammar from standard input:
```cmd
java -jar target/lenguajes-formales.jar
```

With `--sets`, it prints FIRST and FOLLOW sets. Example using input redirection:
```cmd
java -jar target/lenguajes-formales.jar --sets < grammar.txt
```
PowerShell does not support `<`; use a pipe instead:
```powershell
Get-Content grammar.txt | java -jar target/lenguajes-formales.jar --sets
```

Other options:

| Option | What it does |
| --- | --- |
| `--tables` | Reads a grammar and strings (until an empty line) and prints the LL(1) table, the LR(0) automaton, the SLR(1) ACTION/GOTO table and the step-by-step trace of each string. |
| `--sdd` | Item 3 (see below). |
| `--web` | Starts the web visualizer at http://localhost:8080, using the Java classes through a REST API. Stop it with `Ctrl+C`. |

### Input format
- First line: integer `n` (the number of nonterminals).
- Following `n` lines: one line per nonterminal in the format `A -> alt1 alt2` (alternatives separated by blanks).
- `e`: represents epsilon (the empty string).
- `$`: end marker appended automatically.
- Strings to parse: one per line, ending with an empty line.

### Example (Example 2)

Input (`grammar.txt`):
```text
3
S -> AB
A -> aA d
B -> bBc e
```

Expected `--sets` output:
```text
First(A) = {a, d}
First(B) = {b, e}
First(S) = {a, d}
Follow(A) = {b, $}
Follow(B) = {c, $}
Follow(S) = {$}
```

### Parsing strings

After reading the grammar (and printing the sets, if requested), the program decides whether the grammar is LL(1) and whether it is SLR(1):

| Grammar | Output | Then |
| --- | --- | --- |
| LL(1) and SLR(1) | `Select a parser (T: for LL(1), B: for SLR(1), Q: quit):` | Type `T` or `B`, then the strings; an empty line returns to the menu. `Q` quits. |
| Only LL(1) | `Grammar is LL(1).` | Strings are parsed with the LL(1) parser until an empty line. |
| Only SLR(1) | `Grammar is SLR(1).` | Strings are parsed with the SLR(1) parser until an empty line. |
| Neither | `Grammar is neither LL(1) nor SLR(1).` | The program ends. |

Strings are given one per line (without `$`); the empty string is written `e`. For each string the program prints `yes` or `no`.

Example session with Example 2 (input lines are `T`, `d`, `adbc`, `a`, an empty line and `Q`):
```text
Select a parser (T: for LL(1), B: for SLR(1), Q: quit):
yes
yes
no
Select a parser (T: for LL(1), B: for SLR(1), Q: quit):
```

## Item 3 (SDD evaluator)

The SDD computes the decimal value of a number written in a variable base, as `base#digits` (example `8#17` = 15). The grammar:
```text
S -> D#N
N -> DR
R -> DR e
D -> 0 1 2 3 4 5 6 7 8 9
```

The SDD is stored as data in `BaseNumberSdd.java` and the evaluator (`SddEvaluator.java`) is generic.

### How to run

Run the SDD evaluator:
```cmd
java -jar target/lenguajes-formales.jar --sdd
```
Then type strings one per line, ending with an empty line. For each string it prints `no` if the string is not in the language, otherwise the annotated parse tree, the evaluation order (a topological order of the dependency graph) and the value of `S.val`.

### Circular demo

```cmd
java -jar target/lenguajes-formales.jar --sdd --circular
```
This uses a variant of the SDD where the rule `R.acc = D.val` of production `N -> DR` is replaced by `R.acc = N.val`, which makes the dependency graph cyclic; the program reports that the SDD is circular and stops.

Attribute values are `BigInteger`, so long inputs (for example `9#` followed by 40 nines) do not overflow, and the annotated tree is printed iteratively, so very deep trees do not overflow the call stack.

## Use of generative AI

- **Gemini**: Used to explain in depth the concepts, algorithms, and sections of the book (*Compilers: Principles, Techniques, and Tools*, 2nd ed., Aho et al.).
- **Antigravity / Gemini**: Used as a support tool to review the project, verify all algorithms and test cases against the assignment statement, check for potential edge cases, and validate the correct functioning of all items.

## Web visualizer

Build the JAR and run:
```cmd
java -jar target/lenguajes-formales.jar --web
```
Then open http://localhost:8080 in a browser. The page (`src/main/resources/static`) does not compute anything by itself: `app.js` sends each request to the controllers of the `web` package, which call the same classes used by the console program, and only draws the JSON answer. Stop the server with `Ctrl+C`. Its tabs:

- **Gramática**: FIRST/FOLLOW sets, LL(1) table, LR(0) automaton and SLR(1) ACTION/GOTO table, and the step-by-step LL(1) or SLR(1) trace of a string.
- **SDD**: step-by-step evaluation of the Item 3 SDD (annotated tree and evaluation order, including the circular variant).
- **Autómatas**: the previous assignments (see below).

Endpoints:

| Endpoint | Body | Returns |
| --- | --- | --- |
| `POST /api/analyze` | `{"grammar": "..."}` | FIRST/FOLLOW, LL(1) table, LR(0) states and SLR(1) table |
| `POST /api/parse` | `{"grammar": "...", "parser": "LL1" or "SLR1", "input": "..."}` | Step-by-step trace and whether the string is accepted |
| `POST /api/sdd` | `{"input": "8#17", "circular": false}` | Annotated tree, evaluation order and `S.val` |
| `POST /api/automata/convert` | NFA (`states`, `alphabet`, `initial`, `accepting`, `transitions`) | Equivalent DFA |
| `POST /api/automata/simulate` | `{"dfa": <DFA>, "input": "abb"}` | Path of states and whether the string is accepted |
| `POST /api/automata/minimize` | DFA | Equivalent pairs, minimized DFA and the marking table |
| `POST /api/automata/minimize/batch` | Plain text in the format of the assignment statement | One line of equivalent pairs per case |

If the browser still shows an older version of the page, reload it with `Ctrl+F5`.

### NFA to DFA and DFA minimization (previous assignments)

The **Autómatas** tab includes Assignment 1 and Assignment 2:

- **NFA to DFA**: epsilon-closure and subset construction.
- **DFA minimization**: table-filling algorithm (Kozen, Lecture 14), with the marking table and the minimized DFA.
- **DFA simulation**: path of states for an input string.
- **Batch minimization**: the text format of the assignment statement, with one line of equivalent pairs per case. The box starts with the example of the assignment, whose answer is `(4,5)`.

The NFA is written as text:

```text
estados: 0 1 2 3 4 5 6 7 8
alfabeto: a b
inicial: 0
finales: 2 6 8
0 ε 1
0 ε 3
0 ε 7
1 a 2
3 a 4
4 b 5
5 b 6
7 a 7
7 b 8
8 b 8
```

Each transition line is `source symbol target`; `ε` (or `eps`) marks an epsilon transition.

## Report

The report is `Informe.pdf` in the root directory (in Spanish).

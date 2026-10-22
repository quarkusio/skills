# Analyze and Validate Projects via the mtools MCP Server

The `mtools` MCP server is a Quarkus MCP server that exposes the following tools:

| Tool | Description | Parameters |
|------|-------------|------------|
| `sourceAnalyze` | Scan a project for classes, annotations, interfaces, pom dependencies, and properties | `userQueries` (required), `projectPath` |
| `sourceValidate` | Validate a migrated Quarkus project against its Spring source | `target` (required), `source` (required), `validator`, `skipCompile`, `parser` |

## Verify connectivity

Call `tools/list`:
- Both tools present -> Everything's OK.
- Cannot connect -> "Cannot connect to the mtools MCP server. Make sure it is running." STOP
- A tool is missing -> report the missing tool name. STOP

## Analyze application (`sourceAnalyze`)

Call the `sourceAnalyze` MCP tool to scan a project and list its classes, interfaces, annotations, Maven dependencies, and properties.

### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|:--------:|---------|-------------|
| `userQueries` | `string` | **Yes** | — | Semicolon-separated list of query expressions (e.g. `java class all;pom dependency all`). |
| `projectPath` | `string` | No | `.` | Directory path where the project to analyze is located. |

### Query syntax

Query format: `<fileType> <symbol> <operation>`. The operation must be `all`.
`properties all` and `html all` have no symbol (two parts only).

| Query | Scope / Target |
|-------|----------------|
| `java class all` | All Java classes |
| `java interface all` | All Java interfaces |
| `java annotation all` | All Java annotations |
| `java import all` | All Java import statements |
| `pom dependency all` | Maven `pom.xml` dependencies |
| `properties all` | `*.properties` files located directly in `src/{main,test}/resources/` (no symbol) |
| `html all` | `*.html` / `*.htm` files under `src/{main,test}/resources/**` (no symbol) |

### Example invocations

**Targeted query (current directory):**
```json
{
  "userQueries": "pom dependency all"
}
```

**Full scan with explicit project path:**
```json
{
  "projectPath": "<path>",
  "userQueries": "java class all;java interface all;java annotation all;pom dependency all;properties all"
}
```

If we require only specific information (e.g. "list the dependencies"), send only the matching queries.

### Output format

On a successful invocation, the results will be in the following format:

```
Total: <N> match(es) across <M> query(ies)

## Query: <query>
Found <X> match(es):
<one match per line, as returned by the tool>
```

> NOTE: A query without results is reported as "No matches found".

## Validate migration (`sourceValidate`)

Call the `sourceValidate` MCP tool to check a migrated Quarkus project against the original Spring project.

### Parameters

| Parameter | Type | Required | Default | Description |
|-----------|------|:--------:|---------|-------------|
| `target` | `string` | **Yes** | — | Directory path of the migrated Quarkus project. |
| `source` | `string` | **Yes** | — | Directory path of the original Spring project. |
| `validator` | `string` | No | `all` | Specific validator to run: `project-setup`, `persistence`, or `all`. |
| `skipCompile` | `string` | No | `"false"` | When `"true"` (case-insensitive), skip `mvn compile` during validation checks. Any other value means compile is not skipped. |
| `parser` | `string` | No | `"treesitter"` | Parser engine to use for source parsing: `treesitter` or `javaparser`. |

### Available validators

| Validator | Checks |
|-----------|--------|
| `project-setup` | `pom.xml` and `src/main/resources/application.properties` exist, no `@SpringBootApplication` / `@EnableAutoConfiguration` / `@SpringBootTest` in Java files, no `org.springframework*` dependencies in `pom.xml`, POM coordinates (`groupId`, `artifactId`, `version`), `quarkus-maven-plugin`, `quarkus-bom` in `dependencyManagement`, standard Maven directories (`src/{main,test}/{java,resources}`), `mvn compile` |
| `persistence` | Entity count, all entities migrated, table names, ID strategy, fields (type, column), relationships (`mappedBy`, collection type), `mvn compile` |
| `all` | Run all available validators along with their checks. |

### Example invocations

**Full validation (default settings):**
```json
{
  "target": "<quarkus-directory>",
  "source": "<spring-directory>",
  "validator": "all",
  "skipCompile": "false",
  "parser": "treesitter"
}
```

**Targeted validator (skipping compile):**
```json
{
  "target": "<quarkus-directory>",
  "source": "<spring-directory>",
  "validator": "persistence",
  "skipCompile": "true"
}
```

### Output format

On a successful invocation, the results will be in the following format:

```
## <validator name>
Status: <overall status>
Rules: <total> total | <passed> passed | <failed> failed | <warnings> warnings

- [PASS] <rule> — <evidence>
- [FAIL] <rule> — <evidence>
- [WARN] <rule> — <evidence>
```

## Error handling

| Failure | Cause | Action |
|---|---|---|
| "Access denied: path traversal ('..')" | `..` in a path argument | Use a path without `..`. |
| "Access denied: path … is outside the allowed directories" | Path not under `mtool.allowed-dirs` | Tell the user to relaunch with `-Dmtool.allowed-dirs=<dir>` as shown in the message. STOP |
| "Unknown validator: '…'" / "Error: unknown validator '…'" | Name not discovered (validators are lowercase, e.g. `all`, `persistence`, `project-setup`) | Use one from the list in the message, or `all`. |
| "Error: target/source project path does not exist" | Wrong path | Fix the path to point to the correct directory. |
| "Error: query must have at least 2 parts" | Malformed query | Fix the query. |

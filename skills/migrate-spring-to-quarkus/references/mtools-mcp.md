# Analyze and Validate Projects via the mtools MCP Server

The `mtools` MCP server is a Quarkus MCP server that exposes the following tools:

| Tool | Description | Parameters |
|------|-------------|------------|
| `sourceAnalyze` | Scan a project for classes, annotations, interfaces, pom dependencies, properties | `userQueries` (string, `;`-separated), `projectPath` (default `.`) |
| `sourceValidate` | Validate a migrated Quarkus project against its Spring source | `target`, `source`, `validator` (default `all`), `skipCompile` (default `false`), `parser` (default `treesitter`) |

## Verify connectivity

Call `tools/list`.
- Both tools present -> Everything's OK.
- Cannot connect -> "Cannot connect to the mtools MCP server. Make sure it is running." STOP
- A tool is missing -> report the missing tool name. STOP

## Analyze application

Call the `sourceAnalyze` MCP tool to scan a project and list its classes, interfaces, annotations, Maven dependencies and properties.

Query format: `<fileType> <symbol> <operation>`. The operation must be `all`.
Supported queries: `java class all`, `java interface all`, `java annotation all`, `java import all`, `pom dependency all`, `properties all`, `html all`.
`properties all` and `html all` have no symbol (two parts only).

Call the MCP tool `sourceAnalyze` with:
```json
{
  "projectPath": "<path>",
  "userQueries": "java class all;java interface all;java annotation all;pom dependency all;properties all"
}
```

Where the `path` is the directory where the project we want to analyze is located.
If we require only specific information (e.g. "list the dependencies"), send only the matching queries.

On a successful invocation, the results will be in the following format:

```
Total: <N> match(es) across <M> query(ies)

## Query: <query>
Found <X> match(es):
<one match per line, as returned by the tool>
```

> NOTE: A query without results is reported as "No matches found".

## Validate migration

Call the `sourceValidate` MCP tool to check a migrated Quarkus project against the original Spring project.

Call the MCP tool `sourceValidate` with:
```json
{
  "target": "<quarkus-directory>",
  "source": "<spring-directory>",
  "validator": "<validator>",
  "skipCompile": "false"
}
```

Where validator can be:

| Validator | Checks |
|-----------|--------|
| `project-setup` | `pom.xml` and `application.properties`, no Spring Boot files or dependencies, POM coordinates, `quarkus-maven-plugin`, `quarkus-bom`, Maven directories, `mvn compile` |
| `persistence` | Entity count, all entities migrated, table names, ID strategy, fields (type, column), relationships (`mappedBy`, collection type), `mvn compile` |
| `all` | run all available validators along with their checks. |

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
| "Unknown validator" | Name not discovered | Use one from the list in the message, or `all`. |
| "Error: target/source project path does not exist" | Wrong path | Fix the path to point to the correct directory. |
| "Error: query must have at least 2 parts" | Malformed query | Fix the query. |

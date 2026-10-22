# Module: Persistence

Migrate JPA entities and Spring Data repositories to Quarkus Panache (or retain Spring Data JPA under the Spring compatibility extension).

All files to transform are in `<target>` (already copied there by the build module). Do not modify `<source>`.

Read `<target>/migration-spec.yaml` at module start:
- `decisions.strategy`: `full-quarkus` vs `spring-compat`
- `decisions.persistence`: `panache-active-record` vs `panache-repository` vs `hibernate-orm`

Load [references/annotation-map.md](../../references/annotation-map.md) before starting. See the Data section for the full JPA/Panache annotation mapping.

## What to do

- [ ] Migrate entities (JPA → Panache if full Quarkus strategy)
- [ ] Migrate repositories (Spring Data → Panache if full Quarkus strategy)
- [ ] Validate persistence layer. See section: Validation

## Entity Layer (Full Quarkus strategy)

```java
// BEFORE: Spring Data JPA
@Entity
public class Todo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private boolean completed;
    // getters + setters
}

// AFTER: Panache Active Record
@Entity
public class Todo extends PanacheEntity {
    public String title;
    public boolean completed;
    // id provided by PanacheEntity — remove @Id and @GeneratedValue
    // public fields — remove getters/setters
}
```

## Repository Layer (Full Quarkus strategy)

Choose pattern based on `decisions.persistence` in `<target>/migration-spec.yaml`:

**Active Record** (`panache-active-record` — queries on the entity):
```java
// Static methods on the entity
public static List<Todo> findByCompleted(boolean completed) {
    return list("completed", completed);
}
// Usage: Todo.listAll(), Todo.findById(id), Todo.findByCompleted(true)
```

**Repository class** (`panache-repository` — separate from entity):
```java
@ApplicationScoped
public class TodoRepository implements PanacheRepository<Todo> {
    public List<Todo> findByCompleted(boolean completed) {
        return list("completed", completed);
    }
}
```

**Pagination** (replaces Spring's `Page<T>` + `Pageable`):
```java
PanacheQuery<Todo> query = find("completed", completed);
query.page(Page.of(page, size));
long totalCount = query.count();
List<Todo> items = query.list();
```

**Spring compat strategy**: Keep `JpaRepository`/`CrudRepository` — they work with `quarkus-spring-data-jpa`.

## Spring Compat Extension Limitations

When using the compatibility strategy (`quarkus-spring-data-jpa`), be aware of these **verified limitations from the Quarkus source code**:

| Extension | What does NOT work |
|---|---|
| `quarkus-spring-data-jpa` | SpEL `#{...}` in `@Query` not supported. No `Distinct` queries. Limited custom repository fragment support. |

## Validation

- **Read the instructions** of the **MCP tools** definition here: [references/mtools-mcp.md](../../references/mtools-mcp.md)
- **Call** `sourceValidate` tool using arguments as defined part of the MCP Tools definition document
 - Review rules reported under `[PASS]`, `[FAIL]`, and `[WARN]`.
 - Address any `[FAIL]` items (missing entity migrations, mismatched table/column names, ID strategy discrepancies, relationship mappings).
- **If `sourceValidate` tool is not available:**
  - Output message: `source is not available. Skipping automated persistence validation.`
  - Proceed without failing the migration.

## Watch out

- **Missing `@Transactional`**: Quarkus uses `jakarta.transaction.Transactional`, not Spring's. Any write operation outside a resource method must be annotated explicitly
- **No OSIV**: Quarkus doesn't have Open Session in View; lazy loading outside transactions will fail — initialize associations eagerly or use `@Transactional` on the calling method
- **No component scanning**: Beans in external JARs need a Jandex index or `quarkus.index-dependency`

---
name: web-layer-migration-agent
description: Phase 8 Web Layer Migration Agent. Converts Spring MVC @RestController and @Controller to Quarkus JAX-RS @Path resources.
  Migrates HTTP method annotations, parameters, and validates REST contract with RestValidator.
license: Apache-2.0
metadata:
  phase: 8
  agent_type: migration
---

# Phase 8 — Web Layer Migration Agent

## Purpose

Convert Spring MVC/WebFlux controllers (`@RestController`, `@Controller`, `@ControllerAdvice`) to Quarkus REST (RESTEasy Reactive) resources. This phase covers **Java class transformations only**.

> **Phase scope:** Java controller files → JAX-RS resource files.
> Template file migration (syntax, file locations, static assets) is handled by **Phase 8B**.

## ⚠️ CRITICAL: This Phase is ALWAYS Doable - DO NOT SKIP

**DO NOT skip or defer this phase due to complexity, time, tokens, or effort concerns.** Controller migration follows well-established, repeatable patterns:
- **REST APIs**: `@RestController` → `@Path` (annotation mapping)
- **Template controllers**: `@Controller` → `@Path` returning `TemplateInstance`

### Why This Migration is Straightforward

1. **Pattern-Based**: All changes follow clear, repeatable patterns
2. **No Business Logic Changes**: Only framework adaptation, no logic changes
3. **Incremental**: Migrate one controller at a time
4. **Immediate Feedback**: Compilation errors guide you

**Complexity Breakdown:**
- REST controller migration: 5-10 minutes per controller
- Template controller migration (Java side only): 5-10 minutes per controller
- Total time for typical app (5-10 controllers): 1-2 hours

### ⚠️ IMPORTANT: Do Not Skip This Phase

**Even if the migration seems complex or time-consuming:**
- Web layer migration is ESSENTIAL for a functional application
- Skipping this phase leaves the application non-functional
- The work MUST be done eventually - doing it now is more efficient
- Token/time concerns should NOT prevent completing this critical phase

**This phase is required. Complete it before moving forward.**

## ⚠️ CRITICAL: Output File Location

**YOU MUST save the migration report to this exact location:**

```
<quarkus_target_dir>/migration-reports/phase-08-web-migration.json
```

**Before creating the report:**
1. Ensure the `migration-reports/` directory exists (create it if needed)
2. Save the report to the exact path above
3. Do NOT save to the root directory
4. Do NOT use any other filename

## Inputs

- migration-spec.yaml
- Source controller files from repo-metadata.json

## Transformation Rules

Apply rules from transformation_rules.md RULE GROUP 4 — Web Layer Migration.

### Key Transformations

1. **@RestController → @Path**
   ```java
   // Before
   @RestController
   @RequestMapping("/api/orders")
   public class OrderController {}
   
   // After
   @Path("/api/orders")
   public class OrderResource {}
   ```

2. **HTTP Method Mappings**
   ```java
   // Before
   @GetMapping("/{id}")
   public Order getOrder(@PathVariable Long id) {}
   
   // After
   @GET
   @Path("/{id}")
   public Order getOrder(@PathParam("id") Long id) {}
   ```

3. **Parameter Annotations**
   - @PathVariable → @PathParam
   - @RequestParam → @QueryParam
   - @RequestBody → (implicit, no annotation needed)
   - @RequestHeader → @HeaderParam

4. **Response Types**
   ```java
   // Before
   public ResponseEntity<Order> create(@RequestBody Order order) {
       return ResponseEntity.ok(order);
   }
   
   // After
   public Response create(Order order) {
       return Response.ok(order).build();
   }
   ```

5. **Exception Handling**
   ```java
   // Before
   @ExceptionHandler(OrderNotFoundException.class)
   public ResponseEntity<ErrorResponse> handleNotFound(OrderNotFoundException ex) {}
   
   // After
   @Provider
   public class OrderNotFoundExceptionMapper 
       implements ExceptionMapper<OrderNotFoundException> {
       @Override
       public Response toResponse(OrderNotFoundException ex) {}
   }
   ```

### 6. REST Client Migration (CRITICAL)

**⚠️ MANDATORY: Replace Spring RestClient/RestTemplate with JAX-RS Client**

If your code uses Spring's REST client classes, they MUST be migrated to JAX-RS:

```java
// ❌ WRONG - Spring RestClient (will cause runtime ClassNotFoundException)
import org.springframework.web.client.RestClient;

@ApplicationScoped
public class ExternalService {
    private RestClient restClient;
    
    @PostConstruct
    public void init() {
        restClient = RestClient.create(baseUrl);
    }
    
    public List<Data> fetchData() {
        return restClient.get()
            .uri("/data")
            .retrieve()
            .body(new ParameterizedTypeReference<List<Data>>() {});
    }
}

// ✓ CORRECT - JAX-RS Client
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

@ApplicationScoped
public class ExternalService {
    private Client client;
    private WebTarget target;
    
    @PostConstruct
    public void init() {
        client = ClientBuilder.newClient();
        target = client.target(baseUrl);
    }
    
    public List<Data> fetchData() {
        return target.path("data")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<List<Data>>() {});
    }
    
    @PreDestroy
    public void cleanup() {
        if (client != null) {
            client.close();
        }
    }
}
```

**Key Differences:**
- `RestClient.create()` → `ClientBuilder.newClient()`
- `.get().uri()` → `.path().request().get()`
- `ParameterizedTypeReference` → `GenericType`
- Add `@PreDestroy` to close client properly

## Steps

1. Read migration-spec.yaml to get controller list
2. For each controller file:
   a. Read source file
   b. Create target file path (keep same package structure)
   c. Apply transformations:
      - Replace @RestController with @Path
      - Replace @RequestMapping with @Path
      - Replace @GetMapping/@PostMapping/etc with @GET/@POST/etc
      - Replace @PathVariable with @PathParam
      - Replace @RequestParam with @QueryParam
      - Replace @RequestBody (remove annotation)
      - Replace ResponseEntity with Response
      - Update imports
   d. Write transformed file to target
   e. Record in transformation ledger
3. Handle @ControllerAdvice → @Provider ExceptionMapper
4. **Search for Spring REST clients and migrate them:**
   ```bash
   grep -r 'RestClient\|RestTemplate' src/main/java/
   ```
   - Replace Spring RestClient with JAX-RS Client
   - Replace Spring RestTemplate with JAX-RS Client
   - Update all imports
5. **Verify no Spring web dependencies remain:**
   ```bash
   grep -i 'springframework.*web' pom.xml
   ```
   - Remove any temporary Spring web dependencies
6. Run `mvn clean package -DskipTests` to ensure compilation is successful
7. Generate web-migration-report.json

## Import Mappings

```java
// Remove
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

// Add
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
```

## Special Cases

### 1. @Controller (Template-Based)

Convert the Java controller to a JAX-RS resource exactly like `@RestController`, but return `TemplateInstance` instead of a data object:

```java
// Before (Spring MVC)
@Controller
public class WelcomeController {
    @GetMapping("/")
    public String welcome(Model model) {
        model.addAttribute("message", "Welcome");
        return "welcome";  // view name
    }
}

// After (Quarkus)
@Path("/")
public class WelcomeResource {
    @Inject
    Template welcome;  // matches welcome.html in templates/

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance get() {
        return welcome.data("message", "Welcome");
    }
}
```

For templates in subdirectories, use `@io.quarkus.qute.Location("subdir/templateName")` on the injected `Template` field.

**Template file migration** (syntax conversion, file locations, static assets, CSRF removal) is handled by **Phase 8B** — do not duplicate that work here.

### 2. File Uploads
```java
// Before
@PostMapping("/upload")
public String upload(@RequestParam("file") MultipartFile file) {}

// After
@POST
@Path("/upload")
@Consumes(MediaType.MULTIPART_FORM_DATA)
public String upload(@MultipartForm FileUpload file) {}
```

### 3. Async Endpoints
```java
// Before
@GetMapping("/async")
public CompletableFuture<Order> getAsync() {}

// After
@GET
@Path("/async")
public Uni<Order> getAsync() {}
```

## Validation

After transformation:
1. All @RestController classes converted to @Path
2. All HTTP method annotations converted
3. All parameter annotations converted
4. Imports updated
5. Code compiles without errors

## Validation Gate

After completing all transformations, **MANDATORY VALIDATION** must be performed:

**Run validator after completing web layer migration:**

```bash
# Build validator if needed
cd validators/java
mvn clean package -DskipTests -q

# Generate metadata (regenerate each time code changes)
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <spring_source_dir> -o <spring_source_dir>/migration-metadata/code-metadata.yaml
java -jar target/migration-validator-1.0.0.jar extract metadata \
  <quarkus_target_dir> -o <quarkus_target_dir>/migration-metadata/code-metadata.yaml

# Run validator
java -jar target/migration-validator-1.0.0.jar validate rest \
  <spring_source_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir>/migration-metadata/code-metadata.yaml \
  <quarkus_target_dir> \
  <quarkus_target_dir>/migration-spec.yaml
```

**VALIDATION LOOP (MANDATORY - DO NOT SKIP):**
- If validator shows failures (exit code 1):
  1. Read error messages and identify issues
  2. Fix the problems in REST resources
  3. Regenerate metadata and rerun validator
  4. Repeat until exit code = 0 and Status = SUCCESS
- Only proceed to next phase when: `Rules: X total | X passed | 0 failed`

**Validator checks:** @RestController→@Path, HTTP method mappings, parameter annotations (@PathVariable→@PathParam, @RequestParam→@QueryParam), @RequestBody→entity param, response types
    warnings: <count>
```

### Blocking Criteria
The following issues will block progression to Phase 8B:
- Controllers not migrated (@RestController not converted to @Path)
- HTTP method annotations not converted (@GetMapping not converted to @GET)
- Parameter annotations not converted (@PathVariable not converted to @PathParam)
- Endpoint count mismatches (missing endpoints)
- Unmigrated Spring annotations remaining in code

### Non-Blocking Warnings
These can be addressed later but should be documented:
- Path pattern changes (verify intentional)
- Response type differences (verify intentional)
- Missing exception handlers (can be added later)

**⚠️ IMPORTANT: Do not proceed to Phase 8B until validation gate passes!**

## Output

**Directory Setup:**
```bash
mkdir -p migration-reports
```

**File Location:** `migration-reports/phase-08-web-migration.json`

This report should be created in the target Quarkus project at `<quarkus_target_dir>/migration-reports/phase-08-web-migration.json`.

Example:

```json
{
  "phase": "web-layer-migration",
  "status": "completed",
  "controllers_migrated": 8,
  "endpoints_migrated": 45,
  "exception_handlers_migrated": 3,
  "files": [
    {
      "source": "src/main/java/com/example/controller/OrderController.java",
      "target": "src/main/java/com/example/resource/OrderResource.java",
      "endpoints": 5,
      "status": "DONE"
    }
  ],
  "package_status": "PASS",
  "warnings": [],
  "manual_review": []
}
```

## Transformation Ledger

Update migration-spec.yaml transformations.web-migration:

```yaml
transformations:
  web-migration:
    - source: src/main/java/com/example/controller/OrderController.java
      target: src/main/java/com/example/resource/OrderResource.java
      technology: "@RestController → @Path + JAX-RS annotations"
      status: DONE
      notes: "5 endpoints migrated"
```

## Error Handling

On compile errors:
1. Capture error details
2. Attempt automatic fix (missing imports, etc.)
3. If unresolved after 2 attempts, mark for manual review
4. Continue with remaining files
5. Report all issues in web-migration-report.json
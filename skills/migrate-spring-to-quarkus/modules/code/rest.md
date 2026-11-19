# Module: REST

Migrate Spring MVC controllers (returning JSON or HTML) to JAX-RS resources.

Scope: `@Controller`, `@RestController` and the request-mapping annotations. Services, DI and application bootstrap belong to the [service module](service.md). Converting the view templates themselves (Thymeleaf, JSP, etc.) belongs to the frontend module.

All files to transform are in `<target>` (already copied there by the build module). Do not modify `<source>`.

Read `<target>/migration-spec.yaml` at module start:
- `decisions.strategy`: `full-quarkus` vs `spring-compat`

Load [references/annotation-map.md](../../references/annotation-map.md) before starting. Use its REST / Web section.

## What to do

- [ ] Migrate controllers/resources (Spring MVC → JAX-RS if full Quarkus strategy)
- [ ] Migrate `Model.addAttribute()` → Qute `Template.data()` or `@CheckedTemplate`
- [ ] Migrate `return "redirect:..."` → `Response.seeOther()`
- [ ] Compile: `cd <target> && ./mvnw clean compile -DskipTests` (Maven) or `cd <target> && ./gradlew clean compileJava -x test` (Gradle)

Use the annotation-map.md reference for the full mapping. Below are the key patterns with before/after examples.

## Controller → Resource (Full Quarkus strategy)

```java
// BEFORE: Spring MVC
@Controller
public class TodoController {
    @GetMapping("/todos")
    public String list(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todos";
    }

    @PostMapping("/todos")
    public String create(@ModelAttribute Todo todo) {
        todoService.save(todo);
        return "redirect:/todos";
    }
}

// AFTER: Quarkus + JAX-RS + Qute
@Path("/todos")
@ApplicationScoped
public class TodoResource {
    @Inject TodoService todoService;

    @CheckedTemplate
    public static class Templates {
        public static native TemplateInstance todos(List<Todo> todos);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.todos(todoService.findAll());
    }

    @POST
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response create(@BeanParam Todo todo) {
        todoService.save(todo);
        return Response.seeOther(URI.create("/todos")).build();
    }
}
```

**Qute strict data map** — unlike Thymeleaf, Qute throws `TemplateException` if a key referenced in the template is missing from the data map. Every `.data()` call site must provide the **same complete set of keys**, including on empty-result paths.

**Spring compat strategy**: `@RestController` works with `quarkus-spring-web` (but NOT plain `@Controller`).

## Watch out

- **JAX-RS path conflicts**: Spring allows overlapping `@RequestMapping` paths — JAX-RS does not. Check for duplicate `@Path` values

## Spring Compat Extension Limitations

When using the compatibility strategy (`quarkus-spring-web`), be aware of these **verified limitations from the Quarkus source code**:

| Extension | What does NOT work |
|---|---|
| `quarkus-spring-web` | Only `@RestController` — plain `@Controller` not supported. Only one `@RestControllerAdvice` per app. `@CrossOrigin`, `@InitBinder`, `@ModelAttribute` not supported. No reactive types (`Mono`, `Flux`). |

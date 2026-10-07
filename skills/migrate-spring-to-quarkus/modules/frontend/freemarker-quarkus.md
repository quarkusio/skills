# Module: Frontend / View Layer — FreeMarker with Quarkus (quarkus-freemarker)

Preserve the FreeMarker view layer using the Quarkiverse
`io.quarkiverse.freemarker:quarkus-freemarker` extension.

All files to transform are in `<target>`. Do not modify `<source>`.

## What to do

- [ ] Add `io.quarkiverse.freemarker:quarkus-freemarker` using the Quarkus extension tooling (see [Dependencies](#dependencies))
- [ ] Move `.ftl`, `.ftlh`, and `.ftlx` files to `src/main/resources/templates/` (mandatory location for Quarkus FreeMarker)
- [ ] Migrate `spring.freemarker.*` configuration properties to `quarkus.freemarker.*` equivalents (see [Config Migration](#config-migration))
- [ ] Remove `FreeMarkerConfigurer` and `FreeMarkerViewResolver` Spring beans and their imports
- [ ] Migrate controllers: replace Spring MVC `ModelAndView` return with JAX-RS resources using injected `freemarker.template.Configuration` (see [Critical Rules](#critical-rules))
- [ ] Remove `<#import "/spring.ftl" as spring>` directives and all `<@spring.*>` macro calls (see [Spring Macro Removal](#spring-macro-removal))
- [ ] Remove Spring CSRF tokens from FreeMarker templates and JavaScript
- [ ] Move static assets to `src/main/resources/META-INF/resources/`
- [ ] Run the [Validation checklist](#validation-checklist)

## Dependencies

Use the Quarkus extension tooling to automatically resolve the compatible version:

**Maven:**
```bash
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.freemarker:quarkus-freemarker"
```

**Gradle:**
```bash
./gradlew addExtension --extensions="io.quarkiverse.freemarker:quarkus-freemarker"
```

> `io.quarkiverse.freemarker:quarkus-freemarker` is a Quarkiverse extension — it is not part of the Quarkus
> platform BOM. Always use the `quarkus:add-extension` / `addExtension` commands above so the correct version
> compatible with your Quarkus stream is resolved automatically.

## Config Migration

Replace `spring.freemarker.*` properties in `<target>/src/main/resources/application.properties`:

| Spring Boot | Quarkus | Notes |
|---|---|---|
| `spring.freemarker.template-loader-path=classpath:/templates/` | `quarkus.freemarker.base-path=templates` | Path relative to `src/main/resources/`; no `classpath:` prefix |
| `spring.freemarker.default-encoding=UTF-8` | `quarkus.freemarker.default-encoding=UTF-8` | Same value |
| `spring.freemarker.cache=true` | `quarkus.freemarker.enable-cache=true` | Caching toggle |
| `spring.freemarker.suffix=.ftl` | *(remove)* | Not a Quarkus config key — templates are resolved by the full filename passed to `cfg.getTemplate(name)` |
| `spring.freemarker.expose-spring-macro-helpers=true` | *(remove)* | Spring macro helpers are not available in Quarkus |
| `spring.freemarker.expose-request-attributes=true` | *(remove)* | Pass request data explicitly via the model map |

Remove any remaining `spring.freemarker.*` properties that have no Quarkus equivalent.

## Critical Rules

### 1. Template Location

`.ftl`, `.ftlh`, and `.ftlx` files **must** be placed under `src/main/resources/templates/`.
The extension resolves templates relative to `quarkus.freemarker.base-path` (default: `templates`), which maps
to `src/main/resources/templates/` on the classpath.

### 2. Controller Migration Pattern

Spring MVC controllers return a view name string or `ModelAndView`; the framework drives rendering.
In Quarkus, inject `freemarker.template.Configuration` and render the template explicitly:

```java
// BEFORE (Spring MVC):
@Controller
public class ProductController {
    @GetMapping("/products")
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "products";          // resolves to products.ftl
    }
}

// AFTER (Quarkus JAX-RS):
@Path("/products")
@ApplicationScoped
@Produces(MediaType.TEXT_HTML)
public class ProductResource {

    @Inject
    freemarker.template.Configuration cfg;  // injected by quarkus-freemarker

    @Inject
    ProductService productService;

    @GET
    public String list() throws IOException, TemplateException {
        Map<String, Object> model = new HashMap<>();
        model.put("products", productService.findAll());

        Template template = cfg.getTemplate("products.ftl");
        StringWriter writer = new StringWriter();
        template.process(model, writer);
        return writer.toString();
    }
}
```

Key points:
- `cfg.getTemplate(name)` resolves from `quarkus.freemarker.base-path` — use the filename including extension.
- `template.process(model, writer)` throws checked `IOException` and `TemplateException`; declare or wrap them.
- Return the rendered `String` from the JAX-RS method with `@Produces(MediaType.TEXT_HTML)`.

### 3. No `ModelAndView` in Quarkus

`org.springframework.web.servlet.ModelAndView` has no Quarkus equivalent. Remove it entirely.
Pass all data through the `Map<String, Object>` model argument to `template.process()`.

## Spring Macro Removal

The Spring FreeMarker macro library (`/spring.ftl`) is not available in Quarkus. All references must be removed:

```ftl
<!-- REMOVE: import directive -->
<#import "/spring.ftl" as spring>

<!-- REMOVE: message macro — replace with a hardcoded string or pass the message from Java -->
<@spring.message "label.submit"/>

<!-- REMOVE: form macros — replace with plain HTML -->
<@spring.formInput "user.name"/>
```

**Replacements:**

| Spring macro | Quarkus replacement |
|---|---|
| `<@spring.message "key"/>` | Pass the resolved message string from Java via the model map |
| `<@spring.formInput "obj.field"/>` | `<input type="text" name="field" value="${obj.field}">` |
| `<@spring.formTextarea "obj.field"/>` | `<textarea name="field">${obj.field}</textarea>` |
| `<@spring.formSingleSelect "obj.field" options/>` | Plain `<select>` with `<#list options ...>` |
| `<@spring.bind "obj.field"/>` | Remove; bind manually via the model map |

## Validation checklist

- [ ] `quarkus-freemarker` dependency is present in build file
- [ ] No `spring-boot-starter-freemarker` or standalone `freemarker` dependency remains
- [ ] `FreeMarkerConfigurer` and `FreeMarkerViewResolver` beans and imports are removed
- [ ] All `.ftl`, `.ftlh`, and `.ftlx` files are located under `src/main/resources/templates/`
- [ ] No `spring.freemarker.*` properties remain in `application.properties`
- [ ] No `<#import "/spring.ftl" as spring>` directives or `<@spring.*>` macro calls remain
- [ ] Controllers inject `freemarker.template.Configuration` and render via `template.process()`
- [ ] Static assets are placed in `src/main/resources/META-INF/resources/`
- [ ] Spring CSRF tokens (`_csrf`, `_csrf_header`) are removed from templates and JavaScript

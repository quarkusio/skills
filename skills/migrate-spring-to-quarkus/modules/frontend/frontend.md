# Module: Frontend / View Layer

Migrate templates, static assets, and view-related code from Spring to Quarkus.

Load [references/dependency-map.md](../../references/dependency-map.md) before starting.

All files to transform are in `<target>` (already copied there by the build module). Do not modify `<source>`.

Read `<target>/migration-spec.yaml` at module start if it exists:
- If `decisions.view_layer == 'none'`: Skip frontend migration.
- If `decisions.view_layer` is `qute`, `myfaces`, or `freemarker`: Proceed with the steps below.

## Instructions

### Step 1: Detect View Technologies in `<source>`

Inspect `<source>` to determine which view technologies are present:

| Technology | Detection Check in `<source>` | Sub-module to Load |
|---|---|---|
| **Thymeleaf** | `.html` in `src/main/resources/templates/` with `th:*` attributes, or `spring-boot-starter-thymeleaf` in build file | [thymeleaf.md](thymeleaf.md) |
| **JSP** | `.jsp` / `.jspx` in `src/main/webapp/` or `src/main/resources/META-INF/resources/`, `jstl`, or `tomcat-embed-jasper` in build file | [jsp.md](jsp.md) |
| **FreeMarker** | `.ftl` / `.ftlh` / `.ftlx` in `src/main/resources/templates/`, `freemarker.*` imports, `FreeMarkerConfigurer` bean, or `spring-boot-starter-freemarker` | [freemarker.md](freemarker.md) |
| **JSF** | `.xhtml` in `src/main/webapp/` or `src/main/resources/META-INF/resources/`, `faces-config.xml`, `@ManagedBean`, `@FacesConverter`, `@FacesValidator`, `@ViewScoped` | [jsf.md](jsf.md) |

Multiple technologies may be present in the same project. If so, execute the relevant sub-modules in sequence.

### Step 2: Move Static Resources

Move static assets from Spring Boot locations to Quarkus `META-INF/resources`:

```
# BEFORE (Spring Boot / Java EE)
src/main/resources/static/css/style.css
src/main/resources/static/js/app.js
src/main/webapp/images/logo.png

# AFTER (Quarkus)
src/main/resources/META-INF/resources/css/style.css
src/main/resources/META-INF/resources/js/app.js
src/main/resources/META-INF/resources/images/logo.png
```

### Step 3: Remove CSRF Tokens

Quarkus does not use Spring Security's CSRF mechanism in templates. Remove these from HTML and JavaScript:

```html
<!-- DELETE from HTML: -->
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
```

```javascript
// DELETE from JS:
const token = document.querySelector('meta[name="_csrf"]')?.content;
const header = document.querySelector('meta[name="_csrf_header"]')?.content;
```

If the application requires CSRF protection in Quarkus, use `quarkus-rest-csrf`.

### Step 4: Execute Technology Guides

Execute each detected sub-module guide (`thymeleaf.md`, `jsp.md`, `freemarker.md`, `jsf.md`) and verify its Validation Checklist.

### Step 5: Compile

- [ ] Compile: `cd <target> && ./mvnw clean compile -DskipTests` (Maven) or `cd <target> && ./gradlew clean compileJava -x test` (Gradle)

# Module: Frontend / View Layer — Thymeleaf to Qute

Migrate Thymeleaf templates and view-related code from Spring MVC + Thymeleaf to Quarkus + Qute.

All files to transform are in `<target>`. Do not modify `<source>`.

## What to do

- [ ] Ensure `quarkus-rest-qute` dependency is in the build file
- [ ] Convert Thymeleaf templates (`.html`) to Qute syntax
- [ ] Rename template directories to match `@CheckedTemplate` class names (or standard Qute conventions)
- [ ] Ensure all referenced template variables are passed in every `.data()` call path (Qute strict rendering)
- [ ] Remove Spring CSRF tokens from HTML and JavaScript
- [ ] Move static assets to `src/main/resources/META-INF/resources/`
- [ ] Run the [Validation checklist](#validation-checklist)

## Dependency

Use `quarkus-rest-qute` — **never** `quarkus-qute` alone (which lacks REST integration):

**Maven:**
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-qute</artifactId>
</dependency>
```

**Gradle:**
```groovy
implementation 'io.quarkus:quarkus-rest-qute'
```

## Thymeleaf → Qute Syntax Conversion

### Basic Expressions & Attributes

| Thymeleaf | Qute | Notes |
|---|---|---|
| `th:text="${name}"` | `{name}` | Direct expression |
| `th:utext="${html}"` | `{html.raw}` | Unescaped HTML output |
| `th:value="${value}"` | `value="{value}"` | Input value |
| `th:class="${active ? 'on' : 'off'}"` | `class="{active ? 'on' : 'off'}"` | Conditional class |
| `th:attr="data-id=${id},data-name=${name}"` | `data-id="{id}" data-name="{name}"` | Multiple attributes |
| `th:href="@{/path/{id}(id=${item.id})}"` | `href="/path/{item.id}"` | URL with path param |
| `th:action="@{/submit}"` | `action="/submit"` | Form action |

### Control Flow

| Thymeleaf | Qute | Notes |
|---|---|---|
| `th:if="${condition}"` | `{#if condition}...{/if}` | Conditional |
| `th:unless="${condition}"` | `{#if !condition}...{/if}` | Negated conditional |
| `th:each="item : ${items}"` | `{#for item in items}...{/for}` | Loop |
| `th:switch="${value}"` + `th:case` | `{#switch value}{#case 'a'}...{#case 'b'}...{/switch}` | Switch statement |

### Variables & Fragments

| Thymeleaf | Qute | Notes |
|---|---|---|
| `th:with="temp=${value}"` | `{#let temp=value}...{/let}` | Local variable |
| `th:block` | `{#fragment}...{/fragment}` | Non-rendering container |
| `th:fragment="name"` | `{#fragment id=name}...{/fragment}` | Define fragment |
| `th:insert="~{fragments :: name}"` | `{#include fragments$name /}` | Include fragment |
| `th:replace="~{fragments :: name}"` | `{#insert fragments$name /}` | Replace with fragment |

### Boolean Attributes & Form Binding

| Thymeleaf | Qute | Notes |
|---|---|---|
| `th:selected="${isSelected}"` | `{#if isSelected}selected{/if}` | Selected attribute |
| `th:checked="${isChecked}"` | `{#if isChecked}checked{/if}` | Checked attribute |
| `th:disabled="${isDisabled}"` | `{#if isDisabled}disabled{/if}` | Disabled attribute |
| `th:readonly="${isReadonly}"` | `{#if isReadonly}readonly{/if}` | Readonly attribute |
| `th:object="${user}"` | Pass `user` object in template data | No direct equivalent |
| `th:field="*{name}"` | `name="name" value="{user.name}"` | Explicit name/value |
| `th:errors="*{name}"` | `{#if errors.name}{errors.name}{/if}` | Pass errors in data model |

## Template Location & `@CheckedTemplate`

When using `@CheckedTemplate`, template files must match the enclosing resource class:

```
src/main/resources/templates/todos.html       → src/main/resources/templates/TodoResource/todos.html
src/main/resources/templates/todo-detail.html → src/main/resources/templates/TodoResource/todoDetail.html
```

## Qute Strict Data Map (Critical)

Unlike Thymeleaf (which silently evaluates missing variables to null), Qute fails if a variable is missing. **Every `.data()` call site must supply all keys referenced by the template**, including on empty or error branches.

```java
// Empty path must still supply noTasks and empty list
Templates.todos().data("tasks", List.of()).data("noTasks", true);
// Populated path
Templates.todos().data("tasks", tasks).data("noTasks", false);
```

To ease migration, initially set in `application.properties`:
```properties
quarkus.qute.strict-rendering=false
quarkus.qute.property-not-found-strategy=output-original
```
Fix missing variables, then re-enable strict rendering.

## Validation checklist

- [ ] `quarkus-rest-qute` dependency is present in build file
- [ ] No `spring-boot-starter-thymeleaf` or `thymeleaf` dependencies remain
- [ ] No Thymeleaf namespaces or `th:*` attributes remain in HTML templates
- [ ] Template files are moved to `src/main/resources/templates/` (matching `@CheckedTemplate` paths if used)
- [ ] Static assets are placed in `src/main/resources/META-INF/resources/`
- [ ] Spring CSRF tokens (`_csrf`, `_csrf_header`) are removed from templates and JavaScript
- [ ] All render paths in JAX-RS resources supply complete data maps for template expressions

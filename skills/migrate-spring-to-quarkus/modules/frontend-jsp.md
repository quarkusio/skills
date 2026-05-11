# Module: Frontend / JSP to Thymeleaf Migration

Migrate JSP pages and tag libraries to Spring MVC + Thymeleaf as an intermediate step before Quarkus + Qute migration.

## What to do

- [ ] Add `spring-boot-starter-thymeleaf` dependency to `pom.xml`
- [ ] Convert JSP files to Thymeleaf `.html` templates
- [ ] Move templates from `webapp/WEB-INF/jsp/` to `resources/templates/`
- [ ] Replace JSTL tags with Thymeleaf expressions
- [ ] Update controller methods to return logical view names (without `.jsp` extension)
- [ ] Remove JSP-specific configuration (view resolver, prefix/suffix)
- [ ] Convert custom tag libraries to Thymeleaf dialects or utility methods
- [ ] Test rendering in browser
- [ ] Compile: `./mvnw clean compile -DskipTests` or `./gradlew clean compile -x test`

## Why Migrate from JSP to Thymeleaf?

JSP is not supported in Quarkus. The migration path is: **JSP → Thymeleaf → Qute**. Thymeleaf is an intermediate step that allows testing the application in Spring Boot before final Quarkus migration. Reference the [Frontend module](./frontend.md) when performing the migration.

## Dependency

Add `spring-boot-starter-thymeleaf` starter to the project build definition file (`pom.xml` if using Maven, `build.gradle` or `build.gradle.kts` for Gradle).

Remove JSP dependencies (if explicitly declared), such as `jstl` or `jsp-api`.

## JSP → Thymeleaf Syntax Conversion

| JSP / JSTL | Thymeleaf | Notes |
|---|---|---|
| `${name}` | `${name}` or `th:text="${name}"` | Expression in attribute or body |
| `<c:out value="${name}"/>` | `<span th:text="${name}"></span>` | Escaped output |
| `<c:out value="${html}" escapeXml="false"/>` | `<span th:utext="${html}"></span>` | Unescaped HTML |
| `<c:forEach items="${items}" var="item">` | `<div th:each="item : ${items}">` | Loop iteration |
| `<c:if test="${condition}">` | `<div th:if="${condition}">` | Conditional block |
| `<c:choose><c:when test="${x}">...<c:otherwise>` | `<div th:if="${x}">...<div th:unless="${x}">` | Conditional switch |
| `<fmt:formatDate value="${date}" pattern="yyyy-MM-dd"/>` | `<span th:text="${#temporals.format(date, 'yyyy-MM-dd')}"></span>` | Date formatting (`java.time` in Spring Boot 3+) |
| `<fmt:formatNumber value="${price}" pattern="#,##0.00"/>` | `<span th:text="${#numbers.formatDecimal(price, 1, 2)}"></span>` | Number formatting |
| `<c:url value="/path/${id}"/>` | `<a th:href="@{/path/{id}(id=${id})}">` | URL with path variable |
| `<form action="${pageContext.request.contextPath}/submit">` | `<form th:action="@{/submit}">` | Form action |
| `<%@ include file="header.jsp" %>` | `<div th:replace="~{fragments/header :: header}"></div>` | Static include |
| `<jsp:include page="header.jsp"/>` | `<div th:insert="~{fragments/header :: header}"/></div>` | Dynamic include |
| `${pageContext.request.userPrincipal.name}` | `${#authentication.name}` | Security context (requires `thymeleaf-extras-springsecurity6`) |

## Template File Location

```
# BEFORE (JSP)
webapp/WEB-INF/jsp/todos.jsp
webapp/WEB-INF/jsp/todo-detail.jsp
webapp/WEB-INF/views/error.jsp

# AFTER (Thymeleaf)
resources/templates/todos.html
resources/templates/todo-detail.html
resources/templates/error.html
```

## Controller Changes

**Before (JSP):**

```java
@Controller
public class TodoController {
    @GetMapping("/todos")
    public String listTodos(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todos";  // Resolves to /WEB-INF/jsp/todos.jsp
    }
}
```

**After (Thymeleaf):**

```java
@Controller
public class TodoController {
    @GetMapping("/todos")
    public String listTodos(Model model) {
        model.addAttribute("todos", todoService.findAll());
        return "todos";  // Resolves to templates/todos.html
    }
}
```

Return value stays the same — just the file extension and location change.

## Configuration Changes

**Remove JSP view resolver** from `application.properties`:

```properties
# DELETE these lines:
spring.mvc.view.prefix=/WEB-INF/jsp/
spring.mvc.view.suffix=.jsp
```

Spring Boot auto-configures Thymeleaf when `spring-boot-starter-thymeleaf` is on the classpath. Default template location is `classpath:/templates/`.

## Static Assets

Static files can remain in `resources/static/` for Spring Boot. References in templates:

**JSP:**
```jsp
<link href="${pageContext.request.contextPath}/css/style.css" rel="stylesheet"/>
<script src="${pageContext.request.contextPath}/js/app.js"></script>
```

**Thymeleaf:**
```html
<link th:href="@{/css/style.css}" rel="stylesheet"/>
<script th:src="@{/js/app.js}"></script>
```

## Form Handling and CSRF

**JSP with Spring Security CSRF:**
```jsp
<form action="${pageContext.request.contextPath}/submit" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <!-- fields -->
</form>
```

**Thymeleaf (automatic CSRF token injection):**
```html
<form th:action="@{/submit}" method="post">
    <!-- CSRF token automatically added by Spring Security -->
    <!-- fields -->
</form>
```

Thymeleaf automatically adds CSRF tokens to forms when Spring Security is present. No manual token required.

## JSTL Functions → Thymeleaf Utility Objects

| JSTL Function | Thymeleaf Equivalent |
|---|---|
| `${fn:length(items)}` | `${#lists.size(items)}` or `${items.size()}` |
| `${fn:isEmpty(str)}` | `${#strings.isEmpty(str)}` |
| `${fn:toUpperCase(str)}` | `${#strings.toUpperCase(str)}` |
| `${fn:substring(str, 0, 5)}` | `${#strings.substring(str, 0, 5)}` |
| `${fn:contains(str, 'test')}` | `${#strings.contains(str, 'test')}` |
| `${fn:escapeXml(text)}` | Default behavior in `th:text` |

Full reference: [Thymeleaf Expression Utility Objects](https://www.thymeleaf.org/doc/tutorials/3.1/usingthymeleaf.html#appendix-b-expression-utility-objects)

## Custom Tag Libraries

**JSP custom tags** must be replaced:

1. **Simple formatting tags** → Use Thymeleaf utility objects
2. **Complex presentation logic** → Extract to `@Component` beans, inject into templates via model
3. **Reusable fragments** → Use Thymeleaf fragments

**Example: Custom date formatter tag**

```jsp
<!-- JSP custom tag -->
<%@ taglib prefix="custom" uri="http://example.com/tags" %>
<custom:formatDate date="${todo.dueDate}" pattern="long"/>
```

```java
// Spring @Component utility
@Component
public class DateFormatter {
    public String formatLong(LocalDate date) {
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG));
    }
}
```

```java
// Controller
@GetMapping("/todos/{id}")
public String todoDetail(@PathVariable Long id, Model model) {
    model.addAttribute("todo", todoService.findById(id));
    model.addAttribute("dateFormatter", dateFormatter);  // Inject utility
    return "todo-detail";
}
```

```html
<!-- Thymeleaf template -->
<span th:text="${dateFormatter.formatLong(todo.dueDate)}"/>
```

Or use `@ControllerAdvice` with `@ModelAttribute` to make utilities globally available:

```java
@ControllerAdvice
public class GlobalModelAttributes {
    @Autowired
    private DateFormatter dateFormatter;
    
    @ModelAttribute("dateFormatter")
    public DateFormatter dateFormatter() {
        return dateFormatter;
    }
}
```

## Fragments (Reusable Template Blocks)

**JSP includes:**
```jsp
<%@ include file="/WEB-INF/jsp/header.jsp" %>
```

**Thymeleaf fragments:**

`templates/fragments/header.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="header(title)">
    <meta charset="UTF-8"/>
    <title th:text="${title}">Default Title</title>
    <link th:href="@{/css/style.css}" rel="stylesheet"/>
</head>
</html>
```

Usage in main template:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{fragments/header :: header('Todo List')}"></head>
<body>
    <!-- content -->
</body>
</html>
```

## Template Mode and Doctype

Thymeleaf templates must be valid HTML5:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8"/>
    <title th:text="${title}">Page Title</title>
</head>
<body>
    <!-- content -->
</body>
</html>
```

The `xmlns:th` namespace declaration enables IDE autocomplete but is optional at runtime.

## Common Migration Pitfalls

1. **Forgetting `th:` prefix**: `<div if="${x}">` → **wrong**, must be `th:if`
2. **Using scriptlets**: JSP `<% %>` has no Thymeleaf equivalent — extract logic to controller/service
3. **Missing model attributes**: If template references `${user}` but controller doesn't add it, output is often blank/`null`-like; exceptions are more common when dereferencing null values (for example `${user.name}` when `user` is missing).
4. **Form binding**: Use `th:object` and `th:field` for Spring form binding:

```html
<form th:action="@{/todos}" th:object="${todoForm}" method="post">
    <input type="text" th:field="*{title}"/>
    <input type="date" th:field="*{dueDate}"/>
    <button type="submit">Save</button>
</form>
```

## Testing

1. Start the application: `mvn spring-boot:run`
2. Open each migrated page in browser
3. Check for:
   - Missing attributes (500 errors)
   - Broken links/forms (404 errors)
   - Incorrect data rendering
   - CSRF token issues (403 on POST)

## Next Step

Once JSP → Thymeleaf migration is complete and tested, proceed to **Thymeleaf → Qute migration** using the `frontend.md` module.

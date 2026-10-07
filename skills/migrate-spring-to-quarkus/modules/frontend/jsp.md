# Module: Frontend / View Layer — JSP to Qute

Migrate JSP templates, JSTL tags, and view-related code from Spring MVC / Java EE to Quarkus + Qute.

All files to transform are in `<target>`. Do not modify `<source>`.

## What to do

- [ ] Ensure `quarkus-rest-qute` dependency is in the build file
- [ ] Convert `.jsp` files to `.html` Qute templates under `src/main/resources/templates/`
- [ ] Replace JSTL core tags and JSP EL expressions with Qute sections and expressions
- [ ] Replace Spring form tags (`<form:form>`, `<form:input>`, etc.) with standard HTML and Qute expressions
- [ ] Remove JSP directives (`<%@ page %>`, `<%@ taglib %>`, `<%@ include %>`)
- [ ] Remove JSP scriptlets (`<% ... %>`) and declarations (`<%! ... %>`), moving logic to Java resource methods
- [ ] Replace JSP implicit objects (`session`, `request`, `param`, etc.) by passing data explicitly from Java
- [ ] Move static assets from `webapp/` to `src/main/resources/META-INF/resources/`
- [ ] Run the [Validation checklist](#validation-checklist)

## Dependency

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

## JSP / JSTL → Qute Syntax Conversion

### Basic Expressions & Output

| JSP / JSTL | Qute | Notes |
|---|---|---|
| `${name}` / `<c:out value="${name}"/>` | `{name}` | Direct expression |
| `<c:out value="${html}" escapeXml="false"/>` | `{html.raw}` | Unescaped HTML |
| `${user.name}` | `{user.name}` | Property access |
| `${items[0]}` | `{items[0]}` | Array/List index |
| `${empty items}` | `{#if items.isEmpty}...{/if}` | Empty check |
| `${not empty items}` | `{#if !items.isEmpty}...{/if}` | Non-empty check |

### JSP Directives (Remove)

| JSP Directive | Action / Qute Replacement |
|---|---|
| `<%@ page ... %>` | Remove (handled by JAX-RS / HTTP headers) |
| `<%@ taglib ... %>` | Remove (no taglib imports in Qute) |
| `<%@ include file="header.jsp" %>` | `{#include header /}` |
| `<jsp:include page="header.jsp"/>` | `{#include header /}` |

### Control Flow & Variables

| JSTL | Qute | Notes |
|---|---|---|
| `<c:if test="${condition}">...</c:if>` | `{#if condition}...{/if}` | Conditional |
| `<c:choose><c:when test="${x}">...</c:when><c:otherwise>...</c:otherwise></c:choose>` | `{#if x}...{#else}...{/if}` | Branching |
| `<c:forEach items="${items}" var="item">...</c:forEach>` | `{#for item in items}...{/for}` | Iteration |
| `<c:set var="x" value="${y}"/>` | `{#let x=y}...{/let}` | Local variable |
| `<c:url value="/path/${id}"/>` | `href="/path/{id}"` | URL formatting |

### Spring Form Tags

| Spring Form Tag | Standard HTML + Qute |
|---|---|
| `<form:form modelAttribute="user" action="/save">` | `<form method="post" action="/save">` |
| `<form:input path="name"/>` | `<input name="name" value="{user.name}">` |
| `<form:textarea path="bio"/>` | `<textarea name="bio">{user.bio}</textarea>` |
| `<form:errors path="name"/>` | `{#if errors.name}{errors.name}{/if}` |

### JSP Implicit Objects

JSP implicit objects (`request`, `session`, `param`, `cookie`) are not directly accessible in Qute templates. Retrieve values in the JAX-RS resource and pass them via `.data()` or view models:

```java
// BEFORE (JSP): ${sessionScope.user.name} or ${param.id}
// AFTER (JAX-RS + Qute):
@GET
public TemplateInstance show(@QueryParam("id") String id) {
    return Templates.show(userService.findById(id));
}
```

## Formatting & i18n

Move `fmt:formatDate`, `fmt:formatNumber`, and `fn:*` functions into Java pre-computation before passing to the template:

```java
template.data("formattedDate", date.format(DateTimeFormatter.ISO_LOCAL_DATE));
```

For `<fmt:message key="label.welcome"/>`, use Qute i18n `{msg:welcome}`. Qute i18n is built into `quarkus-rest-qute`. To activate `{msg:key}`, define a `@MessageBundle` interface in Java and place localized files under `src/main/resources/messages/`:

```java
package org.acme;

import io.quarkus.qute.i18n.Message;
import io.quarkus.qute.i18n.MessageBundle;

@MessageBundle
public interface AppMessages {
    @Message("Welcome to the application!")
    String welcome();
}
```

Localized overrides can be placed in `src/main/resources/messages/msg_es.properties` (e.g. `welcome=¡Bienvenido a la aplicación!`).

## Validation checklist

- [ ] `quarkus-rest-qute` dependency is present in build file
- [ ] No JSP/JSTL/Jasper dependencies remain (`tomcat-embed-jasper`, `jstl`, `taglibs`)
- [ ] No `.jsp`, `.jspx`, or `.tag` files remain in `<target>` (all converted to `.html` in `src/main/resources/templates/`)
- [ ] All `<%@ page %>`, `<%@ taglib %>`, scriptlets `<% %>`, and expression tags `<%= %>` are removed
- [ ] Spring `<form:*>` tags are converted to plain HTML forms and Qute expressions
- [ ] Static assets are moved from `webapp/` to `src/main/resources/META-INF/resources/`
- [ ] Spring CSRF tokens (`_csrf`, `_csrf_header`) are removed from templates and JavaScript
- [ ] JAX-RS controllers pass all required data explicitly (no reliance on implicit JSP objects)

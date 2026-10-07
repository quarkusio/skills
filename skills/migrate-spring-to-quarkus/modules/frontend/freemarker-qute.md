# Module: Frontend / View Layer — FreeMarker to Qute

Migrate FreeMarker templates, macros, and view-related code from Spring MVC + FreeMarker to Quarkus + Qute.

All files to transform are in `<target>`. Do not modify `<source>`.

## What to do

- [ ] Ensure `quarkus-rest-qute` dependency is in the build file
- [ ] Convert FreeMarker templates (`.ftl`, `.ftlh`, `.ftlx`) to Qute templates (`.html`) under `src/main/resources/templates/`
- [ ] Replace FreeMarker directives (`<#if>`, `<#list>`, `<#assign>`) with Qute sections (`{#if}`, `{#for}`, `{#let}`)
- [ ] Replace FreeMarker expressions (`${...}`) with Qute expressions (`{...}`)
- [ ] Convert FreeMarker macros (`<#macro>`, `<@macro>`) to Qute user tags (`templates/tags/`) or includes
- [ ] Remove FreeMarker configuration beans (`FreeMarkerConfigurer`, `FreeMarkerViewResolver`) and imports
- [ ] Move static assets to `src/main/resources/META-INF/resources/`
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

## FreeMarker → Qute Syntax Conversion

### Basic Expressions & Output

| FreeMarker | Qute | Notes |
|---|---|---|
| `${name}` | `{name}` | Direct expression |
| `${html?no_esc}` | `{html.raw}` | Unescaped HTML output |
| `${user.name}` / `${map.key}` | `{user.name}` / `{map.key}` | Property / map access |
| `${items[0]}` | `{items[0]}` | List indexing |
| `${foo!"default"}` | Handle default in Java or template | Provide value in data model |
| `${foo??}` | `{#if foo??}` or explicit check | Null/existence check |

### Conditionals & Loops

| FreeMarker | Qute | Notes |
|---|---|---|
| `<#if condition>...</#if>` | `{#if condition}...{/if}` | Conditional |
| `<#if a>...<#elseif b>...<#else>...</#if>` | `{#if a}...{#else if b}...{#else}...{/if}` | Multi-branch |
| `<#if items?has_content>` | `{#if !items.isEmpty}` | Non-empty check |
| `<#list items as item>` | `{#for item in items}` | Loop |
| `${item_index}` / `${item_has_next}` | `{item_index}` / `{item_has_next}` | Qute loop metadata |

### Variables & Includes

| FreeMarker | Qute | Notes |
|---|---|---|
| `<#assign x = value>` / `<#local x = value>` | `{#let x=value}...{/let}` | Local variable |
| `<#include "header.ftl">` | `{#include header /}` | Include template |
| `<#import "layout.ftl" as layout>` | Qute user tag or `{#include}` | Template inheritance |

## Macros & User Tags

FreeMarker macros map to Qute User Tags placed under `src/main/resources/templates/tags/`:

```ftl
<!-- BEFORE (FreeMarker macro): alert.ftl -->
<#macro alert message type="info">
<div class="alert alert-${type}">${message}</div>
</#macro>
<@alert message="Saved" type="success"/>
```

```html
<!-- AFTER (Qute User Tag): src/main/resources/templates/tags/alert.html -->
<div class="alert alert-{type ?: 'info'}">{message}</div>

<!-- In page template: -->
{#alert message="Saved" type="success" /}
```

## Spring Macro Migration

Spring FreeMarker macros (`<@spring.message "key"/>`) map to Qute i18n (`{msg:welcome}` or `{msg:key}`). Qute i18n is built into `quarkus-rest-qute`. To activate `{msg:key}`, define a `@MessageBundle` interface in Java and place localized files under `src/main/resources/messages/`:

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

Spring form macros (`<@spring.formInput .../>`) map to standard HTML input elements with Qute expressions.

## Validation checklist

- [ ] `quarkus-rest-qute` dependency is present in build file
- [ ] No FreeMarker dependencies remain (`spring-boot-starter-freemarker`, `freemarker`)
- [ ] No `.ftl`, `.ftlh`, or `.ftlx` files remain (all converted to `.html` in `src/main/resources/templates/`)
- [ ] No `freemarker.*` imports or config classes (`FreeMarkerConfigurer`, `FreeMarkerViewResolver`) remain
- [ ] Reusable macros are converted to Qute user tags in `src/main/resources/templates/tags/`
- [ ] Static assets are placed in `src/main/resources/META-INF/resources/`
- [ ] Spring CSRF tokens (`_csrf`, `_csrf_header`) are removed from templates and JavaScript

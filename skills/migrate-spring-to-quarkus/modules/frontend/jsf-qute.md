# Module: Frontend / View Layer — JSF to Qute

Migrate small JSF applications (< 5 view files) from Jakarta Faces to Quarkus Qute and JAX-RS.

All files to transform are in `<target>`. Do not modify `<source>`.

## What to do

- [ ] Add `quarkus-rest-qute` dependency to the build file
- [ ] Remove JSF dependencies (`jakarta.faces`, `myfaces`, `primefaces`, `omnifaces`, `joinfaces`)
- [ ] Convert `.xhtml` files to Qute `.html` templates under `src/main/resources/templates/`
- [ ] Convert JSF components (`<h:inputText>`, `<h:dataTable>`, `<h:commandButton>`) to standard HTML5 and Qute loops/forms
- [ ] Convert Facelets tags (`ui:composition`, `ui:insert`, `ui:define`, `ui:include`) to Qute includes and insert sections
- [ ] Replace JSF managed beans with JAX-RS Resource methods and DTO / view model records
- [ ] Replace JSF navigation strings with standard REST URLs and redirection
- [ ] Move static assets to `src/main/resources/META-INF/resources/`
- [ ] Delete JSF configuration files (`faces-config.xml`, `web.xml` JSF servlet mappings)
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

## Component & Template Transformations

### Components → HTML + Qute

| JSF Component | HTML + Qute Equivalent |
|---|---|
| `<h:dataTable value="#{users}" var="u">` | `<table>{#for u in users}<tr><td>{u.name}</td></tr>{/for}</table>` |
| `<ui:repeat value="#{users}" var="u">` | `{#for u in users}...{/for}` |
| `<h:panelGroup rendered="#{user.admin}">` | `{#if user.admin}...{/if}` |
| `<h:inputText value="#{user.name}"/>` | `<input name="name" value="{user.name}">` |
| `<h:inputTextarea value="#{user.bio}"/>` | `<textarea name="bio">{user.bio}</textarea>` |
| `<h:commandButton action="#{bean.save}"/>` | `<form method="post" action="/users/save"><button type="submit">Save</button></form>` |
| `<h:outputLink value="users.xhtml">` | `<a href="/users">Users</a>` |

### Facelets → Qute Layout

| Facelets | Qute |
|---|---|
| `<ui:composition template="/layout.xhtml">` | `{#include layout}{#title}Users{/title}{/include}` |
| `<ui:insert name="content"/>` | `{#insert content/}` |
| `<ui:define name="content">` | `{#content}...{/content}` |
| `<ui:include src="/header.xhtml"/>` | `{#include header /}` |

## Managed Beans → JAX-RS Resource

JSF backing beans used for page rendering and action outcomes are replaced with standard JAX-RS resource endpoints:

```java
// BEFORE (JSF Managed Bean):
@Named @RequestScoped
public class UserBean {
    public List<User> getUsers() { return service.findAll(); }
    public String save() { service.save(user); return "users?faces-redirect=true"; }
}

// AFTER (Quarkus JAX-RS + Qute):
@Path("/users")
@ApplicationScoped
public class UserResource {
    @Inject UserService service;

    @CheckedTemplate
    static class Templates {
        static native TemplateInstance list(List<User> users);
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance list() {
        return Templates.list(service.findAll());
    }

    @POST
    @Path("/save")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response save(@BeanParam User user) {
        service.save(user);
        return Response.seeOther(URI.create("/users")).build();
    }
}
```

## Validation checklist

- [ ] `quarkus-rest-qute` dependency is added
- [ ] All JSF dependencies (`jakarta.faces`, `myfaces`, `primefaces`, `omnifaces`, `joinfaces`) are removed
- [ ] No `.xhtml` files remain (all converted to `.html` in `src/main/resources/templates/`)
- [ ] `faces-config.xml` and JSF servlet mappings in `web.xml` are removed
- [ ] JSF annotations (`@ManagedBean`, `@FacesConverter`, `@FacesValidator`, `@ViewScoped`) are removed
- [ ] Static assets are placed in `src/main/resources/META-INF/resources/`
- [ ] Spring CSRF tokens (`_csrf`, `_csrf_header`) are removed from templates and JavaScript
- [ ] Rendering paths and actions are handled via JAX-RS `@Path` endpoints

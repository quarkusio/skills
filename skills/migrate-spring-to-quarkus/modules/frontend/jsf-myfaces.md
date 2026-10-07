# Module: Frontend / View Layer — JSF with Quarkus (MyFaces)

Maintain JSF view layer using the Quarkus Apache MyFaces and PrimeFaces extensions.

All files to transform are in `<target>`. Do not modify `<source>`.

## What to do

- [ ] Add `org.apache.myfaces.core.extensions.quarkus:myfaces-quarkus` (and `io.quarkiverse.primefaces:quarkus-primefaces` if PrimeFaces is used)
- [ ] Move `.xhtml` files to `src/main/resources/META-INF/resources/` (mandatory location for MyFaces in Quarkus)
- [ ] Update XML namespaces in `.xhtml` files from `xmlns.jcp.org` to `jakarta.faces.*`
- [ ] Create or update `web.xml` in `src/main/resources/META-INF/web.xml` with `FacesServlet` mapping
- [ ] Convert `@ManagedBean` to `@Named` + standard CDI scopes (`@RequestScoped`, `@SessionScoped`, `@ApplicationScoped`, `@ViewScoped`)
- [ ] Replace Spring scope annotations (`@SessionScope`, `@RequestScope`) with CDI equivalents
- [ ] Replace `FacesContext` injection with `FacesContext.getCurrentInstance()`
- [ ] Clean `faces-config.xml` of Spring references (remove `SpringBeanFacesELResolver`, Spring security listeners) and update schema to 4.0
- [ ] Add `managed = true` on custom `@FacesConverter` and `@FacesValidator` classes if they use `@Inject`
- [ ] Run the [Validation checklist](#validation-checklist)

## Dependencies

Use the Quarkus extension tooling to automatically resolve compatible versions:

**Maven:**
```bash
# Add MyFaces extension
./mvnw quarkus:add-extension -Dextensions="org.apache.myfaces.core.extensions.quarkus:myfaces-quarkus"

# Add PrimeFaces extension (if using PrimeFaces)
./mvnw quarkus:add-extension -Dextensions="io.quarkiverse.primefaces:quarkus-primefaces"
```

**Gradle:**
```bash
# Add MyFaces extension
./gradlew addExtension --extensions="org.apache.myfaces.core.extensions.quarkus:myfaces-quarkus"

# Add PrimeFaces extension (if using PrimeFaces)
./gradlew addExtension --extensions="io.quarkiverse.primefaces:quarkus-primefaces"
```

## Critical Rules

### 1. File Location
XHTML files **MUST** be placed under `src/main/resources/META-INF/resources/`. Quarkus MyFaces does not serve views from `webapp/` or `resources/templates/`.

### 2. web.xml Configuration
Create `src/main/resources/META-INF/web.xml`:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_5_0.xsd"
         version="5.0">
    <servlet>
        <servlet-name>Faces Servlet</servlet-name>
        <servlet-class>jakarta.faces.webapp.FacesServlet</servlet-class>
        <load-on-startup>1</load-on-startup>
    </servlet>
    <servlet-mapping>
        <servlet-name>Faces Servlet</servlet-name>
        <url-pattern>*.xhtml</url-pattern>
    </servlet-mapping>
</web-app>
```

### 3. CDI Scopes & FacesContext Pattern
Replace Spring scopes and managed properties:

```java
// BEFORE (Spring / JSF managed bean):
@ManagedBean
@SessionScope
public class UserBean {
    @Inject @Qualifier("facesContext") private FacesContext context; // FAILS in Quarkus
    @ManagedProperty("#{userService}") private UserService service;
}

// AFTER (CDI bean):
@Named
@SessionScoped
public class UserBean implements Serializable {
    @Inject UserService service;

    public void doAction() {
        FacesContext context = FacesContext.getCurrentInstance(); // Correct pattern
    }
}
```

## Validation checklist

- [ ] `myfaces-quarkus` dependency is present in build file
- [ ] All `.xhtml` files are located in `src/main/resources/META-INF/resources/`
- [ ] `web.xml` exists in `src/main/resources/META-INF/web.xml` with `FacesServlet` configured
- [ ] XHTML namespaces use `jakarta.faces.*` (not `xmlns.jcp.org`)
- [ ] No Spring scope annotations (`@SessionScope`, `@RequestScope`) remain on backing beans
- [ ] No `FacesContext` injection via `@Inject` remains (uses `FacesContext.getCurrentInstance()`)
- [ ] `faces-config.xml` has Spring EL resolvers and listeners removed
- [ ] ViewScoped beans implement `java.io.Serializable`

## Step 2: Migrate Code

For each Java file that contains annotations identified in Step 1:

1. **Read** the file (if not already in context from Step 1)
2. **Apply** the annotation mapping from the table below
3. **Update** the imports to match the new annotations
4. **Write** the modified file

### What to migrate

- [ ] Replace DI annotations (`@Autowired` → `@Inject`, `@Component` → `@ApplicationScoped`, etc.)
- [ ] Replace REST annotations (`@RestController` → `@Path`, `@GetMapping` → `@GET`, etc.)
- [ ] Replace Spring `@Transactional` with `jakarta.transaction.Transactional`
- [ ] Remove `@SpringBootApplication` main class (only if it just contains `SpringApplication.run()`)
- [ ] Update imports from `org.springframework.*` to their Quarkus/Jakarta equivalents

### Annotation Mapping

#### Dependency Injection

| Spring | Quarkus | Notes |
|--------|---------|-------|
| `@Component` | `@ApplicationScoped` | |
| `@Service` | `@ApplicationScoped` | |
| `@Repository` | `@ApplicationScoped` | |
| `@Configuration` | `@ApplicationScoped` | |
| `@Autowired` | `@Inject` | Field, constructor, setter |
| `@Bean` | `@Produces` | Must be in an `@ApplicationScoped` class |
| `@Value("${prop}")` | `@ConfigProperty(name = "prop")` | |
| `@Qualifier("name")` | `@jakarta.inject.Named("name")` | |
| `@Scope("prototype")` | `@Dependent` | |
| `@PostConstruct` | `@PostConstruct` | Same (jakarta.annotation) |
| `@PreDestroy` | `@PreDestroy` | Same (jakarta.annotation) |

#### REST / Web

| Spring | Quarkus | Notes |
|--------|---------|-------|
| `@RestController` | `@Path("/...")` + `@ApplicationScoped` | |
| `@Controller` | `@Path("/...")` + `@ApplicationScoped` | NOT supported by quarkus-spring-web |
| `@RequestMapping("/path")` | `@Path("/path")` | |
| `@GetMapping` | `@GET` | |
| `@PostMapping` | `@POST` | |
| `@PutMapping` | `@PUT` | |
| `@DeleteMapping` | `@DELETE` | |
| `@PatchMapping` | `@PATCH` | |
| `@PathVariable` | `@PathParam` or `@RestPath` | |
| `@RequestParam` | `@QueryParam` or `@RestQuery` | |
| `@RequestBody` | No annotation needed | Auto JSON deserialization |
| `@RequestHeader` | `@HeaderParam` | |
| `@ResponseBody` | Not needed | Default in Quarkus REST |
| `@ResponseStatus` | Return `Response` with status code | |

#### Data / JPA

| Spring | Quarkus | Notes |
|--------|---------|-------|
| `@Entity`, `@Table`, `@Id`, `@GeneratedValue` | Same | Standard JPA — no change |
| `@Transactional` (Spring) | `@Transactional` (jakarta.transaction) | Change import only |
| `JpaRepository<T,ID>` | `PanacheRepository<T>` | Or keep with quarkus-spring-data-jpa |
| `CrudRepository<T,ID>` | `PanacheRepository<T>` | Or keep with quarkus-spring-data-jpa |
| `@Query("JPQL")` | Panache `find()` or keep with quarkus-spring-data-jpa | |

#### Lifecycle

| Spring | Quarkus | Notes |
|--------|---------|-------|
| `@SpringBootApplication` | Delete class | Quarkus auto-generates main |
| `SpringApplication.run()` | Delete | |
| `CommandLineRunner` | `@Observes StartupEvent` | |
| `@EventListener` | `@Observes` | CDI events |

## Step 3: Report

Present a brief migration report:

```
## Migration Report: [app-name]

### Summary
- Files changed: [list]
- Annotations migrated: [count]
- Files deleted: [list, with reason]

### Changes
| File | What changed |
|------|-------------|

### Leftover Spring imports
[list any remaining org.springframework imports and why they remain]
```

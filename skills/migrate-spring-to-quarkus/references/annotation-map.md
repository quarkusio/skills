# Spring to Quarkus Annotation Map

## Dependency Injection

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-di`) | Notes |
|---|---|---|---|
| `@Component` | `@ApplicationScoped` | Supported → `@Singleton` | Compat maps to `@Singleton` by default, not `@ApplicationScoped` |
| `@Service` | `@ApplicationScoped` | Supported → `@Singleton` | Same as `@Component` in compat |
| `@Repository` | `@ApplicationScoped` | Supported → `@Singleton` | Same as `@Component` in compat |
| `@Autowired` | `@Inject` | Supported → `@Inject` | Field, constructor, and setter injection all supported |
| `@Qualifier("name")` | `@jakarta.inject.Named` or custom `@Qualifier` | Supported → `@Named` | |
| `@Value("${prop}")` | `@ConfigProperty(name = "prop")` | Supported → `@ConfigProperty` | **SpEL `#{...}` NOT supported** — only `${...}` property placeholders |
| `@Value("${prop:default}")` | `@ConfigProperty(name = "prop", defaultValue = "default")` | Supported | Inline defaults work |
| `@Configuration` | `@ApplicationScoped` | Supported → `@ApplicationScoped` | Always application-scoped in compat |
| `@Bean` | `@Produces` | Supported → `@Produces` | **Must be inside `@Configuration` class** — fails otherwise |
| `@Primary` | `@io.quarkus.arc.DefaultBean` or `@Alternative` + `@Priority` | **NOT supported** | Not processed by `quarkus-spring-di` |
| `@Conditional*` | `@IfBuildProfile` or `@LookupIfProperty` | **NOT supported** | Not processed by `quarkus-spring-di` |
| `@Scope("singleton")` | `@Singleton` | Supported | Default scope for stereotypes |
| `@Scope("prototype")` | `@Dependent` | Supported | New instance per injection point |
| `@Scope("request")` | `@RequestScoped` | Supported | Per-HTTP-request lifecycle |
| `@Scope("session")` | `@SessionScoped` | Supported | Per-session lifecycle |
| `@Scope("application")` | `@ApplicationScoped` | Supported | Application-wide singleton with proxy |
| `@Lazy` | No direct equivalent | **NOT supported** | Quarkus beans are lazy by default |
| `@PostConstruct` | `@PostConstruct` | Works (jakarta.annotation) | Same annotation, no mapping needed |
| `@PreDestroy` | `@PreDestroy` | Works (jakarta.annotation) | Same annotation, no mapping needed |

**Compat DI notes:**
- Custom stereotype annotations (meta-annotations extending `@Component`/`@Service`/`@Repository`) are auto-detected
- `List<T>` injection works: `@Autowired List<MyService>` injects all matching beans
- `@Named` alone makes a class a bean (becomes `@Singleton`)

## REST / Web

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-web`) | Notes |
|---|---|---|---|
| `@RestController` | `@Path` + `@ApplicationScoped` | Supported | Only `@RestController`, **NOT plain `@Controller`** |
| `@Controller` | `@Path` + `@ApplicationScoped` | **NOT supported** | Compat only supports `@RestController` |
| `@RequestMapping("/path")` | `@Path("/path")` | Supported | Supports `method`, `produces`, `consumes` attributes |
| `@GetMapping` | `@GET` | Supported | |
| `@PostMapping` | `@POST` | Supported | |
| `@PutMapping` | `@PUT` | Supported | |
| `@DeleteMapping` | `@DELETE` | Supported | |
| `@PatchMapping` | `@PATCH` | Supported | |
| `@PathVariable` | `@PathParam` or `@RestPath` | Supported | Auto-converts types (String, int, Long...) |
| `@RequestParam` | `@QueryParam` or `@RestQuery` | Supported | `required`, `defaultValue`, `Optional<T>` all work |
| `@RequestBody` | No annotation needed | Supported | Auto JSON deserialization |
| `@RequestHeader` | `@HeaderParam` or `@RestHeader` | Supported | |
| `@CookieValue` | `@CookieParam` or `@RestCookie` | Supported | |
| `@MatrixVariable` | `@MatrixParam` | Supported | Not commonly used but supported |
| `@ResponseStatus` | Return `Response` or `RestResponse` | Supported | Works on methods and exception classes |
| `@ExceptionHandler` | `@ServerExceptionMapper` | Supported (in `@RestControllerAdvice`) | **Only one `@RestControllerAdvice` per app** |
| `@RestControllerAdvice` | `@ServerExceptionMapper` on a class | Supported | **Limited to one per application** |
| `@CrossOrigin` | Configure `quarkus.http.cors` in properties | **NOT supported** | Use Quarkus CORS config instead |
| `@ResponseBody` | Default in Quarkus REST | Implicit | |

**Compat Web notes:**
- `ResponseEntity<T>` return type is fully supported
- `ResponseStatusException` can be thrown for error responses
- Wildcard paths supported: `/api/*`, `/ca?s`, `/api/**`
- `@RequestParam Map<String, String>` collects all query params
- `@RequestParam List<String>` splits comma-separated values
- `@InitBinder`, `@ModelAttribute`, `@SessionAttributes` are **NOT supported**

## Data / JPA

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-data-jpa`) | Notes |
|---|---|---|---|
| `@Entity` | `@Entity` | Same | jakarta.persistence — no mapping needed |
| `@Table` | `@Table` | Same | |
| `@Id` / `@EmbeddedId` | Same | Same | |
| `@GeneratedValue` | Same | Same | |
| `@Transactional` | `jakarta.transaction.Transactional` | Same | **NOT Spring's** `@Transactional` |
| `@Query("JPQL")` | Panache `find()` / named queries | Supported | Named `:param` and positional `?1` both work. **SpEL `#{...}` NOT supported** |
| `@Param("name")` | — | Supported | Binds named parameters in `@Query` |
| `@Modifying` | — | Supported | For UPDATE/DELETE queries; returns void, int, or long |
| `CrudRepository<T,ID>` | `PanacheRepository<T>` | Supported | |
| `ListCrudRepository<T,ID>` | `PanacheRepository<T>` | Supported | |
| `JpaRepository<T,ID>` | `PanacheRepository<T>` | Supported | |
| `PagingAndSortingRepository<T,ID>` | `PanacheRepository<T>` | Supported | |
| `ListPagingAndSortingRepository<T,ID>` | `PanacheRepository<T>` | Supported | |
| `@RepositoryDefinition` | — | Supported | Custom repository without extending interface |
| `@NoRepositoryBean` | — | Supported | For intermediate base repository interfaces |

**Compat Spring Data JPA notes:**
- **Derived queries** fully supported: `findBy*`, `countBy*`, `deleteBy*`, `existsBy*`
- **30+ query keywords**: `Is`, `Not`, `IsNull`, `Between`, `LessThan`, `GreaterThan`, `Like`, `StartingWith`, `EndingWith`, `Containing`, `In`, `NotIn`, `True`, `False`, `IsEmpty`, `IsNotEmpty`, `IgnoreCase`, `OrderBy`, etc.
- **Top/First** limiting: `findTop3By*`, `findFirstBy*`
- **Nested property** access: `findByAddress_ZipCode()` traverses relationships
- **Return types**: `Optional<T>`, `List<T>`, `Set<T>`, `Page<T>`, `Slice<T>`, `Stream<T>`, `Streamable<T>`, `long`, `boolean`
- **Pagination**: `Pageable` param → `Page<T>` return
- **Sorting**: `Sort` param supported
- **Batch operations**: `saveAndFlush()`, `deleteAllInBatch()`, `deleteInBatch()`, `deleteAllByIdInBatch()`
- **Fragment/mixin pattern**: repositories can extend custom fragment interfaces with implementations

## Scheduling

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-scheduled`) | Notes |
|---|---|---|---|
| `@Scheduled(cron=...)` | `@io.quarkus.scheduler.Scheduled(cron=...)` | Supported | Property placeholders `${...}` → `{...}` format |
| `@Scheduled(fixedRate=1000)` | `@Scheduled(every="1s")` | Supported | Converted to ISO duration |
| `@Scheduled(fixedDelay=1000)` | `@Scheduled(every="1s")` | **NOT supported** | Throws `IllegalArgumentException` |
| `@Scheduled(initialDelay=5000)` | `@Scheduled(delay=5000, delayUnit=MILLISECONDS)` | Supported | **Cannot combine with cron** |
| `@Schedules({...})` | Multiple `@Scheduled` | Supported | Container annotation unwrapped |
| `@EnableScheduling` | Not needed | Not needed | Auto-enabled with extension |
| `@Async` | Return `Uni<T>` or `CompletionStage` | **NOT supported** | Use managed executor |

## Security

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-security`) | Notes |
|---|---|---|---|
| `@Secured("ROLE_ADMIN")` | `@RolesAllowed("ADMIN")` | Supported | Cannot mix with `@PreAuthorize` on same method |
| `@PreAuthorize("hasRole('ADMIN')")` | `@RolesAllowed("ADMIN")` | Supported (limited SpEL) | See supported expressions below |
| `@EnableWebSecurity` | Not needed | Not needed | Configure in `application.properties` |
| `@AuthenticationPrincipal` | `@Context SecurityContext` or inject `SecurityIdentity` | **NOT supported** | Use Quarkus `SecurityIdentity` |

**Compat `@PreAuthorize` supported expressions:**
- `permitAll()`, `denyAll()`, `isAuthenticated()`, `isAnonymous()`
- `hasRole('ROLE')`, `hasAnyRole('R1', 'R2')`
- `@beanName.methodName()` — bean method returning boolean
- `#paramName == authentication.principal.username` — compare parameter with current user
- `#paramName.property == authentication.principal.username` — compare object property
- `expr1 and expr2`, `expr1 or expr2` — **cannot mix `and` and `or` in same expression**
- **Full SpEL NOT supported** — only the specific patterns above

## Cache

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-cache`) | Notes |
|---|---|---|---|
| `@Cacheable("name")` | `@CacheResult(cacheName = "name")` | Supported | **Single cache name only** — no arrays |
| `@CacheEvict("name")` | `@CacheInvalidate(cacheName = "name")` | Supported | |
| `@CacheEvict(value="name", allEntries=true)` | `@CacheInvalidateAll(cacheName = "name")` | Supported | |
| `@CachePut("name")` | `@CacheInvalidate` + `@CacheResult` | Supported | Invalidates then caches new result |
| `@Caching(...)` | Combine annotations | **NOT supported** | |
| `@CacheConfig` | — | **NOT supported** | |
| `@EnableCaching` | Not needed | Not needed | |

**Compat Cache limitations — these `@Cacheable`/`@CacheEvict` parameters are NOT supported:**
`key`, `keyGenerator`, `cacheManager`, `cacheResolver`, `condition`, `unless`, `sync`, `beforeInvocation`

## Configuration Properties

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-boot-properties`) | Notes |
|---|---|---|---|
| `@ConfigurationProperties(prefix="app")` | `@ConfigMapping(prefix="app")` | Supported | Class-based (with setters) and interface-based |
| `@ConstructorBinding` | `@ConfigMapping` (record/interface) | **NOT supported** | Compat requires no-arg constructor + setters |
| `@EnableConfigurationProperties` | Not needed | Not needed | Auto-detected |
| `@Validated` on config class | `@Valid` | Supported | Auto-validates if Hibernate Validator present |

**Compat `@ConfigurationProperties` notes:**
- Class-based: requires public class with no-arg constructor and setter methods
- Interface-based: only getter methods, uses `@ConfigProperty` for custom names
- Nested objects supported recursively
- `List<T>`, `Set<T>` supported for primitives/enums
- `Map<K,V>` **NOT supported** — throws `DeploymentException`
- Naming: kebab-case by default (`myProperty` → `my-property`)

## Spring Data REST

| Spring | Quarkus (Compat `quarkus-spring-data-rest`) | Notes |
|---|---|---|
| `@RepositoryRestResource` | Supported | Auto-generates CRUD endpoints |
| `@RepositoryRestResource(path="custom")` | Supported | Custom resource path |
| `@RepositoryRestResource(exported=false)` | Supported | Disables REST exposure |
| `@RestResource(exported=false)` | Supported | Disables specific method endpoint |

**Auto-generated endpoints**: `GET /`, `GET /{id}`, `POST /`, `PUT /{id}`, `DELETE /{id}` with pagination and sorting.

## Testing

| Spring | Quarkus (Full Migration) | Compat (`quarkus-spring-boot-test`) | Notes |
|---|---|---|---|
| `@SpringBootTest` | `@QuarkusTest` | **Supported directly** | Works as Quarkus test annotation |
| `@WebMvcTest` | `@QuarkusTest` + RestAssured | — | |
| `@DataJpaTest` | `@QuarkusTest` with test profile | — | |
| `@MockBean` | `@InjectMock` (`io.quarkus.test.InjectMock`) | — | **Moved in 3.2**: old `io.quarkus.test.junit.mockito.InjectMock` deprecated, removed in 4.0 |
| `@TestConfiguration` | `@QuarkusTestResource` | — | |
| `@ActiveProfiles("test")` | `@TestProfile(TestProfile.class)` | — | |
| `TestRestTemplate` | RestAssured (`given().when().get(...)`) | — | |
| `@LocalServerPort` | `@TestHTTPResource` | — | |

## Application Lifecycle

| Spring | Quarkus | Notes |
|---|---|---|
| `@SpringBootApplication` | No equivalent needed | Quarkus auto-discovers beans |
| `SpringApplication.run()` | `Quarkus.run()` or just `quarkus:dev` | Usually no main class needed |
| `CommandLineRunner` | `@Observes StartupEvent` | `io.quarkus.runtime.StartupEvent` |
| `ApplicationRunner` | `@Observes StartupEvent` | |
| `@EventListener` | `@Observes` | CDI events |
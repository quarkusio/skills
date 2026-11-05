# Split the `code` module into `service` and `rest`

- Status: proposed
- Date: 2026-11-05

## Context and Problem Statement

`modules/code/code.md` is the single module for migrating Java source code. It covers two
different concerns: beans and business logic (services, DI, `@Bean`, main class removal),
and HTTP endpoints (controllers to JAX-RS resources).

The skill is adding automated validation through the `quarkit_validate` MCP tool of
m-tool. Each module doc calls one validator, as `persistence.md` does today.
A single `code` module forces one validator to mix unrelated rules. It also has a broad
gate: an app with services but no controllers still runs the controller guidance.

## Decision Drivers

- One module, one validator, with the same scope.
- Narrow gates, so a module (service or rest) is skipped when its layer is absent.
- Smaller context per module.
- Validators built only from existing m-tool methods.

## Considered Options

- **Keep `code.md` and add one `code` validator.** No file moves, but one validator mixes
  unrelated rule sets and the gate stays broad.
- **Keep `code.md` and call several validators from it.** Same broad gate and long doc.
- **Split into `service` and `rest`, each with its own validator.** Proposed.
- **Split into three, with a separate bootstrap module.** More precise by name, but the
  bootstrap content is very small to be put in a separate module.

## Proposal

Replace `modules/code/code.md` with `modules/code/service.md` and `modules/code/rest.md`.
Each module calls its own validator through `quarkit_validate` (`service` and `rest`).

| Module | Scope | Old pipeline source |
|---|---|---|
| `service` | Services, DI, config injection, transactions, scheduling, and application bootstrap (`@Bean`, main class, runners) | `ServiceValidator` |
| `rest` | Spring MVC controllers, returning JSON or HTML, converted to JAX-RS resources | `RestValidator` |

- `rest` is used because it matches the "REST framework" planning decision and the
  old `RestValidator`. `web` would sound like it overlaps with `frontend`, and `core`
  says nothing about content.
- Each module gets its own gate based on the Spring annotations it owns. `service` runs
  before `rest`, as in `code.md` today.
- Import checks are scoped per layer. A blanket "no `org.springframework.*`" rule stays
  in `cleanup`, because controllers still hold Spring web imports after `service` runs.
- The validators reuse existing m-tool code (`JavaAnalyzer`, `TypeQuery`,
  `AnnotationInfo`, the validation model). They need no new scanner, parser or model
  code.

## Consequences

### Positive

- Each module doc and its validator have the same scope.
- Gates are narrower, and agents load less text per module.
- Compat-extension limits sit next to the checks that enforce them.
- The old `ServiceValidator` and `RestValidator` can be adapted in separate changes.

### Negative

- Links to `code.md` must be updated: `SKILL.md`, `cleanup.md`, `README.md`,
  `mtools-mcp.md`, and ADRs 0002 and 0004.
- The hard-coded module list in `SKILL.md` and the validator `if` chain in
  `ValidateTools.java` need manual edits.
- One more compile step per run. In `full-quarkus`, unmigrated controllers can break the
  compile after `service`, which `compile-fix.md` handles.
- Some shared text (reading the spec, the `annotation-map.md` reference) appears in both
  docs.
- The name `service` does not fully describe bootstrap content. The scope line at the
  top of the doc will state it.

### Follow-up

Split the docs and fix all links, then add `ServiceValidator`, then
`RestValidator`. 

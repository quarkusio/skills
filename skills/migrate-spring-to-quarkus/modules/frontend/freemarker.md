# Module: Frontend / View Layer — FreeMarker

Migrate or preserve the FreeMarker view layer from Spring to Quarkus.

## Strategy Resolution

Read `<target>/migration-spec.yaml` at module start if it exists:

| Condition | Strategy | Sub-module to Execute |
|---|---|---|
| `decisions.view_layer == 'freemarker'` | Preserve FreeMarker with Quarkus | [freemarker-quarkus.md](freemarker-quarkus.md) |
| `decisions.view_layer == 'qute'` | Migrate FreeMarker to Qute | [freemarker-qute.md](freemarker-qute.md) |
| `decisions.strategy == 'spring-compat'` (or standalone `spring-compat`) | Preserve FreeMarker with Quarkus | [freemarker-quarkus.md](freemarker-quarkus.md) |
| `decisions.strategy == 'full-quarkus'` (or standalone default) | Migrate FreeMarker to Qute | [freemarker-qute.md](freemarker-qute.md) |

## Strategy Execution

1. **If `decisions.view_layer` is set**:
   - `freemarker` → load and execute [freemarker-quarkus.md](freemarker-quarkus.md)
   - `qute` → load and execute [freemarker-qute.md](freemarker-qute.md)
2. **Else fallback to migration strategy** (`decisions.strategy` or standalone strategy):
   - `spring-compat` → load and execute [freemarker-quarkus.md](freemarker-quarkus.md)
   - `full-quarkus` (or default) → load and execute [freemarker-qute.md](freemarker-qute.md)

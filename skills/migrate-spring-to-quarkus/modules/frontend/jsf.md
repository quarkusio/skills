# Module: Frontend / View Layer — JSF / Jakarta Faces

Migrate JSF (Jakarta Faces) view layer from Spring to Quarkus.

## Strategy Resolution

Read `<target>/migration-spec.yaml` at module start if it exists:

| Condition | Strategy | Sub-module to Execute |
|---|---|---|
| `decisions.view_layer == 'myfaces'` | Maintain JSF with Quarkus MyFaces | [jsf-myfaces.md](jsf-myfaces.md) |
| `decisions.view_layer == 'qute'` | Migrate JSF to Quarkus Qute | [jsf-qute.md](jsf-qute.md) |
| `decisions.strategy == 'spring-compat'` (or standalone `spring-compat`) | Maintain JSF with Quarkus MyFaces | [jsf-myfaces.md](jsf-myfaces.md) |
| `decisions.strategy == 'full-quarkus'` (or standalone default) | Migrate JSF to Quarkus Qute | [jsf-qute.md](jsf-qute.md) |

## Strategy Execution

1. **If `decisions.view_layer` is set**:
   - `myfaces` → load and execute [jsf-myfaces.md](jsf-myfaces.md)
   - `qute` → load and execute [jsf-qute.md](jsf-qute.md)
2. **Else fallback to migration strategy** (`decisions.strategy` or standalone strategy):
   - `spring-compat` → load and execute [jsf-myfaces.md](jsf-myfaces.md)
   - `full-quarkus` (or default) → load and execute [jsf-qute.md](jsf-qute.md)

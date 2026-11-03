---
name: <skill-name>
description: >
  <What the skill does. Include trigger keywords for auto-invocation.>
---

# <Skill Title>

<One-line summary: what this skill does.>

## Common instructions

- Follow [common/mcp-tools.md](common/mcp-tools.md) for MCP tool detection, calling, and fallback.
- Follow [common/logging.md](common/logging.md) for logging rules and templates.

## Available Tools

<!-- List every MCP tool the skill depends on. Prefix tool names when the
     agent may be connected to multiple MCP servers (e.g., greeting_greetHello). -->

| Tool | Description | Parameters |
|------|-------------|------------|
| `<toolName>` | <What it does> | `<param>` (<type>, default: `<value>`) |

## Steps

<!-- Write MCP tool calls as MANDATORY steps, not conditional.
     Do NOT use "if available" or "if connected" — the agent will silently skip the call.
     Handle failures in the Error Handling section instead. -->

### Step 1: <Action description>

Call `<toolName>` with:
```json
{ "<param>": "<value>" }
```

<Describe what to do with the result.>

### Step N: Present Results

<Format and display the output to the user.>

## Error Handling

<!-- Only handle actual runtime failures — not discovery or connectivity checks.
     The MCP protocol handles tool discovery automatically via tools/list. -->

| Failure | Action |
|---------|--------|
| Tool returns an error response | Report the error message to the user |
| Connection failure | Report: "MCP server is not reachable. Ensure the server is running and accessible." **STOP** |
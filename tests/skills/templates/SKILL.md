---
name: analyze
description: >
  Analyze an application project.
  Use when the user asks to "Analyze this application", "analyze the following code", "analyze".
---

# Analyze an application project

Analyze an application project using as argument the path.
Exit after: "List the project files"

## Common instructions

- Follow [common/mcp-tools.md](common/mcp-tools.md) for MCP tool detection, calling, and fallback.
- Follow [common/logging.md](common/logging.md) for logging rules and templates.

## Available Tools

| Tool | Description | Parameters |
|------|-------------|------------|
| `analyzeSource` | Analyze source code of the project by listing files (excluding target/) | `path` (string, default: `.`) — path to the project |

## Steps

### Step 1: Call `analyzeSource`

Call `analyzeSource` with:
```json
{ "path": "<project-path>" }
```

Use the project path provided by the user.

### Step 2: Present Results

Display the tool output to the user.
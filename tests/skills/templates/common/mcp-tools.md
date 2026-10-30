# MCP Tools — Shared Instructions

## Prompt Detection

MCP tools are called when the user's prompt signals intent — look for phrases such as:
- "use tools", "use MCP", "deep analysis", "full analysis"
- An explicit tool name from the skill's Available Tools table

When none of these signals are detected, use agent tools instead.

## How to Call

1. Log the intent in the analysis log (tool name, arguments, why).
2. Call the tool using `mcp__quarkus-agent__quarkus_callTool` with the tool name and arguments.
3. Log the result summary in the analysis log.
4. Incorporate the tool's output into your analysis.
5. **Do not** duplicate this work with agent tools when the MCP tool already returns the result.

## Error Handling & Fallback

When an MCP tool call fails, **do not stop**. Fall back to equivalent agent tools, and log the fallback.

| Failure | Log entry |
|---------|-----------|
| Tool returns an error response | `[ACTION N] MCP tool <NAME> failed: <ERROR>. Falling back to agent tools.` |
| Connection failure | `[ACTION N] MCP server not reachable. Falling back to agent tools.` |
| MCP not requested by user | `[ACTION N] MCP tools not requested. Using agent tools.` |
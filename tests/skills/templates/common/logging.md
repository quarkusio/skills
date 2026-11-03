# Logging — Shared Instructions

## Rules

- Create at the beginning of the session a log file: **ai-analysis-log_timestamp.txt** using the template below.
- For every **action** you take including every **tool** you execute, **log it** in the log file.
- Every entry will include:
  - The action or thought description
  - The tool executed when applicable
  - Why you decided to take such an action/decision
- Show the path of the **ai-analysis-log_timestamp.txt** file created end of the session

## Log Template

```txt
AI Analysis Log - <TIMESTAMP>
Project: <PATH>
MCP Tools: <ENABLED | DISABLED>
================================================================================

[ACTION 1] Get timestamp for log filename
  Tool: Bash (date command)
  Why: Need a unique timestamp to name the log file as requested

[ACTION 2] Create this log file
  Tool: Write
  Why: Begin logging all actions as the user requested
```

## Log Format — MCP Tool Calls

```txt
[ACTION N] Call MCP tool: <TOOL_NAME>
  Tool: mcp__quarkus-agent__quarkus_callTool
  Arguments: <ARGS>
  Why: <REASON>
  Result: <SUMMARY OF OUTPUT>
```

## Log Format — Fallback

```txt
[ACTION N] MCP tool <TOOL_NAME> failed: <ERROR>. Falling back to agent tools.
  Tool: Bash / Read
  Why: MCP call failed, performing equivalent work with agent tools
  Result: <SUMMARY OF OUTPUT>
```
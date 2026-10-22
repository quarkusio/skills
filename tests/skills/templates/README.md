## SKILL templates

This folder contains templates designed specifically to evaluate/test:

- MCP Server and Tools
- Log AI actions, thoughts, tools used

## Instructions

- Open a terminal and launch `mtool` MCP Server
```shell
cd /path/to/migration-tools/mcp
export ALLOWED_DIR=/path/to/migration-tools/tests/projects
mvn -Dmtool.allowed-dirs=$ALLOWED_DIR \
  -Dquarkus.mcp.server.traffic-logging.enabled=true \
  quarkus:dev
```
- Open another terminal and launch the following ACP command
```shell
cd /path/to/quarkus-skills/tests/skills/templates
acp run -a claude-acp \
  --mcp-server-config mcp-server.json \
  -s SKILL.md \
  --backup no \
  --wks $ALLOWED_DIR/ \
  -p "Analyze the project: spring-jpa-crud with tools."
```

**Note**: Feel free to replay the scenario without the MCP server, without text " ... with tools" to check what AI will generate. Look too to the generate log report
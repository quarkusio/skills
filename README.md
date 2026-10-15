# Quarkus Skills

Agent skills project for developing, maintaining Quarkus applications or migrating.

The [tests](./tests) folder is a Java AI framework that you can use to run a skill, define a project to be tested, apply checks on the project migrated. It uses [Smallrye ACP Java Lib](https://github.com/smallrye/smallrye-acp-client) to run an ACP agent from a registry.

## Available Skills

| Skill | Category | Triggers |
|-------|----------|----------|
| [quarkus-update](#quarkus-update) | Quarkus | "check project", "update quarkus", "quarkus upgrade" |
| [migrate-spring-to-quarkus](#migrate-spring-to-quarkus) | Migration | "spring to quarkus", "quarkus migration", "replace spring" |
| [scaffold-quarkus-mcp-server](#scaffold-quarkus-mcp-server) | Scaffolding | "create mcp server", "scaffold mcp", "quarkus mcp" |

<details>
<summary><strong>quarkus-update</strong></summary>

Check if a Quarkus project's build files are up-to-date by comparing against reference generated projects. Use this skill when you want to:

- Check if your Quarkus project is up-to-date
- Compare your build configuration against the latest Quarkus version
- Upgrade your Quarkus version with guidance on what changes to make

</details>

<details>
<summary><strong>migrate-spring-to-quarkus</strong></summary>

Migrate Spring Boot applications to Quarkus using a modular, gate-driven approach. Supports both Spring compatibility extensions and full Quarkus migration paths. Use this skill when you want to:

- Migrate a Spring Boot application to Quarkus
- Convert Spring annotations (DI, REST, Data, Security) to Quarkus equivalents
- Migrate Spring build files, configuration, frontend (Thymeleaf/JSP), and tests

</details>

<details>
<summary><strong>scaffold-quarkus-mcp-server</strong></summary>

Scaffold a complete Quarkus MCP server project using LTS Quarkus release and the `quarkus-mcp-server` Quarkiverse extension. Use this skill when you want to:

- Create a new Quarkus MCP server project with tools, resources, and prompts
- Generate a project skeleton bootstrapped via code.quarkus.io
- Get MCP Tools java tests and README included out of the box

</details>

## Installation

### Using npx skills add

Install all skills from this repository:

```bash
npx skills add quarkusio/skills
```

List available skills:

```bash
npx skills add quarkusio/skills --list
```

Install specific skills only:

```bash
npx skills add quarkusio/skills --skill quarkus-update
```

Install for specific agents:

```bash
npx skills add quarkusio/skills -a claude-code
```

### Using Claude Code Plugin System

Within Claude Code, you can also install via the plugin marketplace:

```
/plugin marketplace add quarkusio/skills
/plugin install quarkus-appdev@quarkus-skills
```

## Learn More

- [npx skills documentation](https://github.com/vercel-labs/skills)
- [Claude Code skills documentation](https://code.claude.com/docs/en/plugin-marketplaces)

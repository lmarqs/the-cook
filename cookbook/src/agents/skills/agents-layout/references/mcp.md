# Share an MCP server

Add, change, or remove the server by name in `.agents/mcp/servers.yaml`. Each top-level key is one server.
`#seed` starts the file with `{}`. In an existing layout, create the file when adding the first server.

## Choose the transport and credentials

```yaml
reports:
  url: https://reports.example.org/mcp
  oauth:
    clientId: reports-public-client
    callbackPort: 3118
    scopes: [reports.read]
issues:
  url: https://issues.example.org/mcp
  bearerTokenEnv: ISSUES_MCP_TOKEN
search:
  command: search-mcp
  args: ["--index", "project files"]
  envFrom: [SEARCH_MCP_KEY]
```

| Field | Meaning |
| --- | --- |
| `type` | Optional. `http` takes a `url`. `stdio` takes a `command`. |
| `url`, `headers` | HTTP endpoint and optional map of literal request header values. |
| `bearerTokenEnv` | Environment variable holding the HTTP bearer token. Use a service-specific name. |
| `oauth.clientId`, `oauth.callbackPort` | Public OAuth client ID and fixed callback port for this server. Use the port registered for the client. |
| `oauth.scopes` | List of requested scopes. Cook joins them with spaces for Claude and writes a list for Codex. |
| `command`, `args` | Stdio executable and optional ordered list of argument strings. A single argument can contain spaces. |
| `env`, `envFrom` | Map of literal stdio environment values and list of variable names to forward from the developer's environment. |

For credentials, commit only the variable names. Each developer sets the values in the environment that launches
the client. For OAuth, the developer completes sign-in through the client. The OAuth fields were checked against
Claude Code 2.1.330 and Codex 0.171.0, including the per-server `oauth.callback_port` in Codex.
[Claude Code MCP configuration](https://code.claude.com/docs/en/mcp) ·
[Codex 0.171.0 schema](https://github.com/openai/codex/blob/rust-v0.171.0/codex-rs/core/config.schema.json)

## Adopt an existing native entry

1. For Claude, copy the named entry from the `mcpServers` map in the root `.mcp.json` into the YAML.
   Convert a space-separated OAuth scopes string to a list. Convert bearer-token references to
   `bearerTokenEnv` and forwarded stdio variables to `envFrom`.
2. For Codex, if the same name already has a `[mcp_servers.<name>]` table, rewrite that table as an inline
   value under `[mcp_servers]` before cooking. For example, use `reports = { url = "https://reports.example.org/mcp" }`.
   Keep all existing fields so the dry run exposes differences. Cook owns the entire named entry.
   Other servers can keep their table headers.

## Apply and verify a change

1. Find the team's `#connect` version and arguments in `cook.lock`. Preview the cook with every harness the
   team uses. For a two-client project, use the following form.

   ```sh
   cook --dry-run io.github.lmarqs.cookbook:agents:<version>#connect --harness=claude --harness=codex
   ```

2. Check each selected client's diff for the intended additions, changes, or removals. Entries under other
   names should be preserved. Apply the same command without `--dry-run`.
3. Commit the YAML, rendered client files, and updated `cook.lock` together. Open `/mcp` in each client
   from a trusted project and check each expected server's status.
